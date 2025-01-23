package pl.allegro.tech.hermes.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.profiling.ConsumerRun;
import pl.allegro.tech.hermes.consumers.consumer.profiling.DefaultConsumerProfiler;
import pl.allegro.tech.hermes.consumers.consumer.profiling.Measurement;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class ConsumerProfilingTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @RegisterExtension
  public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

  private ListAppender<ILoggingEvent> listAppender;

  @BeforeEach
  void createLogAppender() {
    Logger logWatcher = (Logger) LoggerFactory.getLogger(DefaultConsumerProfiler.class);

    listAppender = new ListAppender<>();
    listAppender.start();

    logWatcher.addAppender(listAppender);
  }

  @AfterEach
  void teardown() {
    ((Logger) LoggerFactory.getLogger(DefaultConsumerProfiler.class)).detachAndStopAllAppenders();
    hermes.clearManagementData();
  }

  @AfterAll
  static void teardownClass() {
    hermes.clearManagementData();
  }

  @Test
  public void shouldNotProfileWhenProfilingIsDisabled() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                    .withProfilingEnabled(false)
                    .build());
    TestMessage message = TestMessage.random();
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

    // when
    subscriber.waitUntilReceived(message.body());

    // then
    List<ILoggingEvent> logsList =
        listAppender.list.stream()
            .filter(
                log ->
                    log.getFormattedMessage().contains(subscription.getQualifiedName().toString()))
            .toList();
    assertThat(logsList).hasSize(0);
  }

  @Test
  public void shouldProfileEmptyRun() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

    // when
    Subscription subscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName()).withProfilingEnabled(true).build());

    // then
    waitAtMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              List<ILoggingEvent> logsList =
                  listAppender.list.stream()
                      .filter(
                          log ->
                              log.getFormattedMessage()
                                  .contains(subscription.getQualifiedName().toString()))
                      .toList();
              assertThat(logsList).hasSizeGreaterThan(0);
              assertThat(logsList.get(0).getFormattedMessage())
                  .contains(
                      String.format(
                          "Consumer profiler measurements for subscription %s and %s run:",
                          subscription.getQualifiedName(), ConsumerRun.EMPTY),
                      Measurement.SIGNALS_AND_SEMAPHORE_ACQUIRE,
                      Measurement.MESSAGE_RECEIVER_NEXT,
                      Measurement.MESSAGE_CONVERSION,
                      "partialMeasurements",
                      Measurement.SIGNALS_INTERRUPT_RUN);
            });
  }

  @Test
  public void shouldNotProfileRunsBelowThreshold() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                    .withProfilingEnabled(true)
                    .withProfilingThresholdMs(100_000)
                    .build());
    TestMessage message = TestMessage.random();
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

    // when
    subscriber.waitUntilReceived(message.body());

    // then
    List<ILoggingEvent> logsList =
        listAppender.list.stream()
            .filter(
                log ->
                    log.getFormattedMessage().contains(subscription.getQualifiedName().toString()))
            .toList();
    assertThat(logsList).hasSize(0);
  }

  @Test
  public void shouldProfileSuccessfulMessageProcessing() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                    .withProfilingEnabled(true)
                    .build());
    TestMessage message = TestMessage.random();
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

    // when
    subscriber.waitUntilReceived(message.body());

    // then
    waitAtMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              List<ILoggingEvent> logsList =
                  listAppender.list.stream()
                      .filter(
                          log -> log.getFormattedMessage().contains(ConsumerRun.DELIVERED.name()))
                      .toList();
              assertThat(logsList).hasSizeGreaterThan(0);
              assertThat(logsList.get(0).getFormattedMessage())
                  .contains(
                      String.format(
                          "Consumer profiler measurements for subscription %s and %s run:",
                          subscription.getQualifiedName(), ConsumerRun.DELIVERED),
                      Measurement.SIGNALS_AND_SEMAPHORE_ACQUIRE,
                      Measurement.MESSAGE_RECEIVER_NEXT,
                      Measurement.MESSAGE_CONVERSION,
                      Measurement.OFFER_INFLIGHT_OFFSET,
                      Measurement.TRACKERS_LOG_INFLIGHT,
                      Measurement.SCHEDULE_MESSAGE_SENDING,
                      Measurement.ACQUIRE_RATE_LIMITER,
                      Measurement.MESSAGE_SENDER_SEND,
                      Measurement.HANDLERS,
                      "partialMeasurements",
                      Measurement.SIGNALS_INTERRUPT_RUN);
            });
  }

  @Test
  public void shouldProfileDiscardedMessageProcessing() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    TestSubscriber subscriber = subscribers.createSubscriber(400);
    Subscription subscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                    .withProfilingEnabled(true)
                    .build());
    TestMessage message = TestMessage.random();
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

    // when
    subscriber.waitUntilReceived(message.body());

    // then
    waitAtMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              List<ILoggingEvent> logsList =
                  listAppender.list.stream()
                      .filter(
                          log -> log.getFormattedMessage().contains(ConsumerRun.DISCARDED.name()))
                      .toList();
              assertThat(logsList).hasSizeGreaterThan(0);
              assertThat(logsList.get(0).getFormattedMessage())
                  .contains(
                      String.format(
                          "Consumer profiler measurements for subscription %s and %s run:",
                          subscription.getQualifiedName(), ConsumerRun.DISCARDED),
                      Measurement.SIGNALS_AND_SEMAPHORE_ACQUIRE,
                      Measurement.MESSAGE_RECEIVER_NEXT,
                      Measurement.MESSAGE_CONVERSION,
                      Measurement.OFFER_INFLIGHT_OFFSET,
                      Measurement.TRACKERS_LOG_INFLIGHT,
                      Measurement.SCHEDULE_MESSAGE_SENDING,
                      Measurement.ACQUIRE_RATE_LIMITER,
                      Measurement.MESSAGE_SENDER_SEND,
                      Measurement.HANDLERS,
                      "partialMeasurements",
                      Measurement.SIGNALS_INTERRUPT_RUN);
            });
  }

  @Test
  public void shouldProfileRetriedMessageProcessing() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    TestMessage message = TestMessage.random();
    TestSubscriber subscriber = subscribers.createSubscriberWithRetry(message.body(), 1);
    Subscription subscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                    .withProfilingEnabled(true)
                    .build());
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

    // when
    subscriber.waitUntilReceived(Duration.ofSeconds(5), 2);

    // then
    waitAtMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              List<ILoggingEvent> retriedLogsList =
                  listAppender.list.stream()
                      .filter(log -> log.getFormattedMessage().contains(ConsumerRun.RETRIED.name()))
                      .toList();
              assertThat(retriedLogsList).hasSizeGreaterThan(0);
              assertThat(retriedLogsList.get(0).getFormattedMessage())
                  .contains(
                      String.format(
                          "Consumer profiler measurements for subscription %s and %s run:",
                          subscription.getQualifiedName(), ConsumerRun.RETRIED),
                      Measurement.SIGNALS_AND_SEMAPHORE_ACQUIRE,
                      Measurement.MESSAGE_RECEIVER_NEXT,
                      Measurement.MESSAGE_CONVERSION,
                      Measurement.OFFER_INFLIGHT_OFFSET,
                      Measurement.TRACKERS_LOG_INFLIGHT,
                      Measurement.SCHEDULE_MESSAGE_SENDING,
                      Measurement.ACQUIRE_RATE_LIMITER,
                      Measurement.MESSAGE_SENDER_SEND,
                      Measurement.HANDLERS,
                      "partialMeasurements",
                      Measurement.SIGNALS_INTERRUPT_RUN);
            });

    // and
    waitAtMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              List<ILoggingEvent> processedLogsList =
                  listAppender.list.stream()
                      .filter(
                          log -> log.getFormattedMessage().contains(ConsumerRun.DELIVERED.name()))
                      .toList();
              assertThat(processedLogsList).hasSizeGreaterThan(0);
              assertThat(processedLogsList.get(0).getFormattedMessage())
                  .contains(
                      String.format(
                          "Consumer profiler measurements for subscription %s and %s run:",
                          subscription.getQualifiedName(), ConsumerRun.DELIVERED),
                      Measurement.SCHEDULE_RESEND,
                      Measurement.MESSAGE_SENDER_SEND,
                      Measurement.HANDLERS,
                      "retryDelayMillis 1000");
            });
  }
}
