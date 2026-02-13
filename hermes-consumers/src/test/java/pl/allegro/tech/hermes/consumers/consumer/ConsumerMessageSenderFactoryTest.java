package pl.allegro.tech.hermes.consumers.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.subscription.metrics.MessageProcessingDurationMetricOptions;
import pl.allegro.tech.hermes.api.subscription.metrics.SubscriptionMetricsConfig;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.metric.SubscriptionHermesCounter;
import pl.allegro.tech.hermes.common.metric.SubscriptionMetrics;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.consumer.offset.PendingOffsets;
import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.result.DefaultErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.DefaultSuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.ResultHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesHistogram;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;
import pl.allegro.tech.hermes.tracker.consumers.deadletters.DeadLetters;

@RunWith(MockitoJUnitRunner.class)
public class ConsumerMessageSenderFactoryTest {

  @Mock private MessageSenderFactory messageSenderFactory;
  @Mock private Trackers trackers;
  @Mock private DeadLetters deadLetters;
  @Mock private FutureAsyncTimeout futureAsyncTimeout;
  @Mock private UndeliveredMessageLog undeliveredMessageLog;
  @Mock private Clock clock;
  @Mock private InstrumentedExecutorServiceFactory instrumentedExecutorServiceFactory;
  @Mock private ConsumerAuthorizationHandler consumerAuthorizationHandler;
  @Mock private ResultHandler supportedHandler;
  @Mock private ResultHandler unsupportedHandler;
  @Mock private Subscription subscription;
  @Mock private SubscriptionLoadRecorder subscriptionLoadRecorder;
  @Mock private MetricsFacade metrics;
  @Mock private SubscriptionMetrics subscriptionMetrics;
  @Mock private SerialConsumerRateLimiter consumerRateLimiter;
  @Mock private PendingOffsets pendingOffsets;
  @Mock private SubscriptionMetricsConfig subscriptionMetricsConfig;

  @Before
  public void setUp() {
    when(instrumentedExecutorServiceFactory.getExecutorService(
            anyString(), anyInt(), anyBoolean()))
        .thenReturn(mock(ExecutorService.class));

    SubscriptionName subscriptionName = new SubscriptionName("subscription", new TopicName("group", "topic"));
    when(subscription.getQualifiedName()).thenReturn(subscriptionName);
    when(subscription.getMetricsConfig()).thenReturn(subscriptionMetricsConfig);
    when(subscriptionMetricsConfig.messageProcessing()).thenReturn(MessageProcessingDurationMetricOptions.DISABLED);

    when(metrics.subscriptions()).thenReturn(subscriptionMetrics);

    when(supportedHandler.appliesTo(subscription)).thenReturn(true);
    when(unsupportedHandler.appliesTo(subscription)).thenReturn(false);
    when(consumerAuthorizationHandler.appliesTo(subscription)).thenReturn(true);

    SubscriptionPolicy subscriptionPolicy = mock(SubscriptionPolicy.class);
    when(subscription.getSerialSubscriptionPolicy()).thenReturn(subscriptionPolicy);
    when(subscriptionPolicy.getRequestTimeout()).thenReturn(1000);

    mockMetrics();
  }

  private void mockMetrics() {
    when(subscriptionMetrics.throughputInBytes(any())).thenReturn(mock(SubscriptionHermesCounter.class));
    when(subscriptionMetrics.successes(any())).thenReturn(mock(HermesCounter.class));
    when(subscriptionMetrics.inflightTimeInMillisHistogram(any())).thenReturn(mock(HermesHistogram.class));
    when(subscriptionMetrics.messageProcessingTimeInMillisHistogram(any(), any(MessageProcessingDurationMetricOptions.class))).thenReturn(mock(HermesTimer.class));
    when(subscriptionMetrics.latency(any())).thenReturn(mock(HermesTimer.class));
    when(subscriptionMetrics.retries(any())).thenReturn(mock(HermesCounter.class));
  }

  @Test
  public void shouldCreateConsumerMessageSenderWithFilteredHandlers() throws Exception {
    // given
    ConsumerMessageSenderFactory factory = new ConsumerMessageSenderFactory(
        "kafka-cluster",
        messageSenderFactory,
        trackers,
        deadLetters,
        futureAsyncTimeout,
        undeliveredMessageLog,
        clock,
        instrumentedExecutorServiceFactory,
        consumerAuthorizationHandler,
        List.of(supportedHandler, unsupportedHandler),
        1000,
        1,
        false
    );

    // when
    ConsumerMessageSender sender = factory.create(
        subscription,
        consumerRateLimiter,
        pendingOffsets,
        subscriptionLoadRecorder,
        metrics
    );

    // then
    List<SuccessHandler> successHandlers = getField(sender, "successHandlers");
    List<ErrorHandler> errorHandlers = getField(sender, "errorHandlers");
    List<ErrorHandler> discardedHandlers = getField(sender, "discardedHandlers");

    assertThat(successHandlers)
        .hasSize(3) // auth, default, supported
        .contains(supportedHandler)
        .contains(consumerAuthorizationHandler)
        .doesNotContain(unsupportedHandler)
        .anyMatch(h -> h instanceof DefaultSuccessHandler);

    assertThat(errorHandlers)
        .hasSize(2) // auth, default
        .contains(consumerAuthorizationHandler)
        .anyMatch(h -> h instanceof DefaultErrorHandler);

    assertThat(discardedHandlers)
        .hasSize(3) // auth, default, supported
        .contains(consumerAuthorizationHandler)
        .contains(supportedHandler)
        .doesNotContain(unsupportedHandler)
        .anyMatch(h -> h instanceof DefaultErrorHandler);
  }

  private <T> T getField(Object object, String fieldName) throws Exception {
    Field field = object.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return (T) field.get(object);
  }
}
