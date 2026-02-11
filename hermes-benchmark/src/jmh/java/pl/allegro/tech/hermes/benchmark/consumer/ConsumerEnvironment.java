package pl.allegro.tech.hermes.benchmark.consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import pl.allegro.tech.hermes.api.ContentType;
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
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.consumers.supervisor.process.ConsumerProcess;
import pl.allegro.tech.hermes.consumers.supervisor.process.Signal;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.NoOpConsumerNodeLoadRegistry;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;
import pl.allegro.tech.hermes.tracker.consumers.deadletters.DeadLetters;

@State(Scope.Thread)
public class ConsumerEnvironment {

  private static final int MESSAGES_COUNT = 100_000;
  private static final Clock CLOCK = Clock.systemUTC();
  private Subscription subscription;
  private Topic topic;
  private InMemoryDelayedMessageSender messageSender;
  private ThreadPoolExecutor executorService;
  private ConsumerProcess consumerProcess;
  private FutureAsyncTimeout futureAsyncTimeout;
  private InstrumentedExecutorServiceFactoryWrapper instrumentedExecutorServiceFactory;

  @Setup(Level.Iteration)
  public void setupEnvironment() {
    topic = createTopic();
    subscription = createSubscription(topic);
    messageSender = new InMemoryDelayedMessageSender();
    futureAsyncTimeout = new FutureAsyncTimeout(Executors.newSingleThreadScheduledExecutor());
    instrumentedExecutorServiceFactory =
        new InstrumentedExecutorServiceFactoryWrapper(new MetricsFacade(new SimpleMeterRegistry()));
    executorService = createExecutor();
    consumerProcess = createConsumerProcess();
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

  private ConsumerProcess createConsumerProcess() {
    NoOpConsumerAuthorizationHandler authorizationHandler = new NoOpConsumerAuthorizationHandler();
    InMemoryProtocolMessageSenderProvider messageSenderProvider =
        new InMemoryProtocolMessageSenderProvider(messageSender);
    ConsumerMessageSenderFactory consumerMessageSenderFactory =
        createMessageSenderFactory(authorizationHandler, messageSenderProvider);
    SerialConsumer serialConsumer = createSerialConsumer(consumerMessageSenderFactory);
    return new ConsumerProcess(
        Signal.of(Signal.SignalType.START, subscription.getQualifiedName()),
        serialConsumer,
        null,
        CLOCK,
        Duration.ofSeconds(60),
        (subscriptionName) -> {});
  }

  private SerialConsumer createSerialConsumer(
      ConsumerMessageSenderFactory consumerMessageSenderFactory) {
    return new SerialConsumer(
        new InMemoryReceiverFactory(MESSAGES_COUNT),
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
        MESSAGES_COUNT);
  }

  private Subscription createSubscription(Topic topic) {
    SubscriptionPolicy subscriptionPolicy =
        new SubscriptionPolicy(15000, 300, 1000, 1000, false, 100, 160, 0, 1, 600);

    return subscription(topic.getQualifiedName(), "subscription")
        .withState(Subscription.State.ACTIVE)
        .withContentType(ContentType.JSON)
        .withSubscriptionPolicy(subscriptionPolicy)
        .build();
  }

  private static Topic createTopic() {
    return topicWithRandomName().withContentType(ContentType.AVRO).build();
  }

  public void waitUntilAllMessagesAreConsumed() {
    await()
        .atMost(adjust(Duration.ofSeconds(10)))
        .untilAsserted(
            () -> assertThat(messageSender.getSentMessagesCount()).isEqualTo(MESSAGES_COUNT));
  }

  public void startConsumer() {
    executorService.submit(() -> consumerProcess.run());
  }

  @TearDown(Level.Iteration)
  public void shutdown() {
    consumerProcess.accept(Signal.of(Signal.SignalType.STOP, subscription.getQualifiedName()));
    executorService.shutdown();
    futureAsyncTimeout.shutdown();
    messageSender.shutdown();
    instrumentedExecutorServiceFactory.shutdownAll();
  }
}
