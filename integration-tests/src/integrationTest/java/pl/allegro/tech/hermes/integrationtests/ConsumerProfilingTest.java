package pl.allegro.tech.hermes.integrationtests;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jayway.awaitility.Duration;
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

import java.util.List;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class ConsumerProfilingTest {

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

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
    }

    @Test
    public void shouldNotProfileWhenProfilingIsDisabled() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        Subscription subscription = hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                .withProfilingEnabled(false).build());
        TestMessage message = TestMessage.random();
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // when
        subscriber.waitUntilReceived(message.body());

        // then
        List<ILoggingEvent> logsList = listAppender.list.stream()
                .filter(log -> log.getFormattedMessage().contains(subscription.getQualifiedName().toString())).toList();
        assertThat(logsList).hasSize(0);
    }

    @Test
    public void shouldProfileEmptyRun() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        // when
        Subscription subscription = hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName())
                .withProfilingEnabled(true).build());

        // then
        waitAtMost(Duration.TEN_SECONDS).until(() -> {
            List<ILoggingEvent> logsList = listAppender.list.stream()
                    .filter(log -> log.getFormattedMessage().contains(subscription.getQualifiedName().toString())).toList();
            assertThat(logsList).hasSizeGreaterThan(0);
            assertThat(logsList.get(0).getFormattedMessage()).contains(
                    String.format("Flushing measurements for subscription %s and %s run:", subscription.getQualifiedName(), ConsumerRun.EMPTY),
                    Measurement.SIGNALS_AND_SEMAPHORE_ACQUIRE.getDescription(),
                    Measurement.MESSAGE_RECEIVER_NEXT.getDescription(),
                    Measurement.MESSAGE_CONVERSION.getDescription(),
                    "partialMeasurements",
                    Measurement.SIGNALS_INTERRUPT_RUN.getDescription()
            );
        });
    }

    @Test
    public void shouldProfileSuccessfulMessageProcessing() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        Subscription subscription = hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                .withProfilingEnabled(true).build());
        TestMessage message = TestMessage.random();
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // when
        subscriber.waitUntilReceived(message.body());

        // then
        List<ILoggingEvent> logsList = listAppender.list.stream()
                .filter(log -> log.getFormattedMessage().contains(ConsumerRun.PROCESSED.name())).toList();
        assertThat(logsList.get(0).getFormattedMessage()).contains(
                String.format("Flushing measurements for subscription %s and %s run:", subscription.getQualifiedName(), ConsumerRun.PROCESSED),
                Measurement.SIGNALS_AND_SEMAPHORE_ACQUIRE.getDescription(),
                Measurement.MESSAGE_RECEIVER_NEXT.getDescription(),
                Measurement.MESSAGE_CONVERSION.getDescription(),
                Measurement.OFFER_INFLIGHT_OFFSET.getDescription(),
                Measurement.TRACKERS_LOG_INFLIGHT.getDescription(),
                Measurement.ACQUIRE_RATE_LIMITER.getDescription(),
                Measurement.MESSAGE_SENDER_SEND.getDescription(),
                Measurement.HANDLERS.getDescription(),
                "partialMeasurements",
                Measurement.SIGNALS_INTERRUPT_RUN.getDescription()
        );
    }

    @Test
    public void shouldProfileDiscardedMessageProcessing() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSubscriber subscriber = subscribers.createSubscriber(400);
        Subscription subscription = hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                .withProfilingEnabled(true)
                .build());
        TestMessage message = TestMessage.random();
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // when
        subscriber.waitUntilReceived(message.body());

        // then
        List<ILoggingEvent> logsList = listAppender.list.stream()
                .filter(log -> log.getFormattedMessage().contains(ConsumerRun.DISCARDED.name())).toList();
        assertThat(logsList.get(0).getFormattedMessage()).contains(
                String.format("Flushing measurements for subscription %s and %s run:", subscription.getQualifiedName(), ConsumerRun.DISCARDED),
                Measurement.SIGNALS_AND_SEMAPHORE_ACQUIRE.getDescription(),
                Measurement.MESSAGE_RECEIVER_NEXT.getDescription(),
                Measurement.MESSAGE_CONVERSION.getDescription(),
                Measurement.OFFER_INFLIGHT_OFFSET.getDescription(),
                Measurement.TRACKERS_LOG_INFLIGHT.getDescription(),
                Measurement.ACQUIRE_RATE_LIMITER.getDescription(),
                Measurement.MESSAGE_SENDER_SEND.getDescription(),
                Measurement.HANDLERS.getDescription(),
                "partialMeasurements",
                Measurement.SIGNALS_INTERRUPT_RUN.getDescription()
        );
    }

    @Test
    public void shouldProfileRetriedMessageProcessing() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestMessage message = TestMessage.random();
        TestSubscriber subscriber = subscribers.createSubscriberWithRetry(message.body(), 1);
        Subscription subscription = hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                .withProfilingEnabled(true)
                .build());
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // when
        subscriber.waitUntilReceived(Duration.FIVE_SECONDS, 2);

        // then
        List<ILoggingEvent> retriedLogsList = listAppender.list.stream()
                .filter(log -> log.getFormattedMessage().contains(ConsumerRun.RETRIED.name())).toList();
        assertThat(retriedLogsList.get(0).getFormattedMessage()).contains(
                String.format("Flushing measurements for subscription %s and %s run:", subscription.getQualifiedName(), ConsumerRun.RETRIED),
                Measurement.SIGNALS_AND_SEMAPHORE_ACQUIRE.getDescription(),
                Measurement.MESSAGE_RECEIVER_NEXT.getDescription(),
                Measurement.MESSAGE_CONVERSION.getDescription(),
                Measurement.OFFER_INFLIGHT_OFFSET.getDescription(),
                Measurement.TRACKERS_LOG_INFLIGHT.getDescription(),
                Measurement.ACQUIRE_RATE_LIMITER.getDescription(),
                Measurement.MESSAGE_SENDER_SEND.getDescription(),
                Measurement.HANDLERS.getDescription(),
                "partialMeasurements",
                Measurement.SIGNALS_INTERRUPT_RUN.getDescription()
        );

        // and
        List<ILoggingEvent> processedLogsList = listAppender.list.stream()
                .filter(log -> log.getFormattedMessage().contains(ConsumerRun.PROCESSED.name())).toList();
        assertThat(processedLogsList.get(0).getFormattedMessage()).contains(
                String.format("Flushing measurements for subscription %s and %s run:", subscription.getQualifiedName(), ConsumerRun.PROCESSED),
                Measurement.SCHEDULE_RESEND.getDescription(),
                Measurement.MESSAGE_SENDER_SEND.getDescription(),
                Measurement.HANDLERS.getDescription(),
                "retryDelayMillis 1000"
        );
    }
}
