package pl.allegro.tech.hermes.benchmark.consumer;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.openjdk.jmh.infra.Blackhole;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.config.CommonConsumerProperties;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerMessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.SerialConsumer;
import pl.allegro.tech.hermes.consumers.consumer.converter.AvroToJsonMessageConverter;
import pl.allegro.tech.hermes.consumers.consumer.converter.DefaultMessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.converter.NoOperationMessageConverter;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.consumers.supervisor.process.ConsumerProcess;
import pl.allegro.tech.hermes.consumers.supervisor.process.Signal;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.NoOpConsumerNodeLoadRegistry;
import pl.allegro.tech.hermes.domain.filtering.MessageFilters;
import pl.allegro.tech.hermes.domain.filtering.SubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.domain.filtering.avro.AvroPathSubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.domain.filtering.json.JsonPathSubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;
import pl.allegro.tech.hermes.tracker.consumers.deadletters.DeadLetters;

public class ConsumerEnvironment {

  private static final Clock CLOCK = Clock.systemUTC();
  private Subscription subscription;
  private Topic topic;
  private final InMemoryDelayedMessageSender messageSender;
  private ThreadPoolExecutor executorService;
  private ConsumerProcess consumerProcess;
  private final FutureAsyncTimeout futureAsyncTimeout;
  private final InstrumentedExecutorServiceFactoryWrapper instrumentedExecutorServiceFactory;
  private InMemoryMessageReceiver inMemoryMessageReceiver;

  public ConsumerEnvironment(Blackhole blackhole) {
    messageSender = new InMemoryDelayedMessageSender(blackhole);
    futureAsyncTimeout = new FutureAsyncTimeout(Executors.newSingleThreadScheduledExecutor());
    instrumentedExecutorServiceFactory =
        new InstrumentedExecutorServiceFactoryWrapper(new MetricsFacade(new SimpleMeterRegistry()));
  }

  public void createConsumer(int messagesCount) {
    this.inMemoryMessageReceiver =
        new InMemoryMessageReceiver(UserAvroMessageProducer.defaultProducer(), messagesCount);
    InMemoryReceiverFactory messageReceiverFactory =
        new InMemoryReceiverFactory(this.inMemoryMessageReceiver);
    topic = createTopic();
    subscription = createSubscription(topic, null);
    consumerProcess =
        createConsumerProcess(messageReceiverFactory, inMemoryMessageReceiver, messagesCount);
    executorService = createExecutor();
  }

  private ThreadPoolExecutor createExecutor() {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    executor.prestartAllCoreThreads();
    return executor;
  }

  private ConsumerMessageSenderFactory createMessageSenderFactory(
      NoOpConsumerAuthorizationHandler consumerAuthorizationHandler,
      InMemoryProtocolMessageSenderProvider messageSenderProvider) {
    return new ConsumerMessageSenderFactory(
        "test",
        new MessageSenderFactory(List.of(messageSenderProvider)),
        new Trackers(List.of()),
        new DeadLetters(List.of()),
        futureAsyncTimeout,
        new NoOpUndeliveredMessageLog(),
        CLOCK,
        instrumentedExecutorServiceFactory,
        consumerAuthorizationHandler,
        1000,
        1,
        false);
  }

  private ConsumerProcess createConsumerProcess(
      ReceiverFactory messageReceiverFactory,
      InMemoryMessageReceiver inMemoryMessageReceiver,
      int messagesCount) {
    NoOpConsumerAuthorizationHandler authorizationHandler = new NoOpConsumerAuthorizationHandler();
    InMemoryProtocolMessageSenderProvider messageSenderProvider =
        new InMemoryProtocolMessageSenderProvider(messageSender);
    ConsumerMessageSenderFactory consumerMessageSenderFactory =
        createMessageSenderFactory(authorizationHandler, messageSenderProvider);
    SerialConsumer serialConsumer =
        createSerialConsumer(
            messageReceiverFactory,
            inMemoryMessageReceiver,
            consumerMessageSenderFactory,
            messagesCount);
    return new ConsumerProcess(
        Signal.of(Signal.SignalType.START, subscription.getQualifiedName()),
        serialConsumer,
        null,
        CLOCK,
        Duration.ofSeconds(60),
        (subscriptionName) -> {});
  }

  private SerialConsumer createSerialConsumer(
      ReceiverFactory messageReceiverFactory,
      InMemoryMessageReceiver inMemoryMessageReceiver,
      ConsumerMessageSenderFactory consumerMessageSenderFactory,
      int messagesCount) {
    this.inMemoryMessageReceiver = inMemoryMessageReceiver;

    return new SerialConsumer(
        messageReceiverFactory,
        new MetricsFacade(new SimpleMeterRegistry()),
        subscription,
        new NoOpConsumerRateLimiter(),
        consumerMessageSenderFactory,
        new Trackers(List.of()),
        new DefaultMessageConverterResolver(
            new AvroToJsonMessageConverter(), new NoOperationMessageConverter()),
        topic,
        new CommonConsumerProperties(),
        new NoOpConsumerAuthorizationHandler(),
        new NoOpConsumerNodeLoadRegistry.NoOpSubscriptionLoadRecorder(),
        new ConsumerPartitionAssignmentState(),
        Duration.ofSeconds(5),
        messagesCount);
  }

  private Subscription createSubscription(
      Topic topic, @Nullable MessageFilterSpecification filterSpecification) {
    SubscriptionPolicy subscriptionPolicy =
        new SubscriptionPolicy(15000, 300, 1000, 1000, false, 100, 160, 0, 1, 600);

    SubscriptionBuilder subscriptionBuilder =
        subscription(topic.getQualifiedName(), "subscription")
            .withState(Subscription.State.ACTIVE)
            .withContentType(ContentType.JSON)
            .withSubscriptionPolicy(subscriptionPolicy);
    if (filterSpecification != null) {
      subscriptionBuilder.withFilter(filterSpecification);
    }
    return subscriptionBuilder.build();
  }

  private static Topic createTopic() {
    return topicWithRandomName().withContentType(ContentType.AVRO).build();
  }

  public void waitUntilAllMessagesAreConsumed(int expectedMessagesSentCount) {
    await()
        .atMost(adjust(Duration.ofSeconds(2)))
        .untilAsserted(
            () -> {
              assertThat(messageSender.getSentMessagesCount()).isEqualTo(expectedMessagesSentCount);
              assertThat(inMemoryMessageReceiver.getQueuedMessagesCount()).isEqualTo(0);
            });
  }

  public void startConsumer() {
    executorService.submit(() -> consumerProcess.run());
  }

  public void stopConsumer() {
    consumerProcess.accept(Signal.of(Signal.SignalType.STOP, subscription.getQualifiedName()));
    messageSender.reset();
    executorService.shutdown();
  }

  public void shutDownAfterAll() {
    messageSender.shutdown();
    futureAsyncTimeout.shutdown();
    instrumentedExecutorServiceFactory.shutdownAll();
  }

  public void createFilteringConsumer(
      UserAvroMessageProducer messageProducer,
      MessageFilterSpecification filterSpecification,
      int messagesCount) {
    this.inMemoryMessageReceiver = new InMemoryMessageReceiver(messageProducer, messagesCount);
    InMemoryFilteringReceiverFactory messageReceiverFactory = createFilteringReceiverFactory();
    topic = createTopic();
    subscription = createSubscription(topic, filterSpecification);
    consumerProcess =
        createConsumerProcess(messageReceiverFactory, inMemoryMessageReceiver, messagesCount);
    executorService = createExecutor();
  }

  private InMemoryFilteringReceiverFactory createFilteringReceiverFactory() {
    List<SubscriptionMessageFilterCompiler> subscriptionFilterCompilers =
        List.of(
            new JsonPathSubscriptionMessageFilterCompiler(),
            new AvroPathSubscriptionMessageFilterCompiler());
    FilterChainFactory filterChainFactory =
        new FilterChainFactory(new MessageFilters(emptyList(), subscriptionFilterCompilers));
    return new InMemoryFilteringReceiverFactory(inMemoryMessageReceiver, filterChainFactory);
  }
}
