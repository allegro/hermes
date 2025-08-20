package pl.allegro.tech.hermes.consumers.consumer;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.consumer.offset.PendingOffsets;
import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.result.DefaultErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.DefaultSuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

public class ConsumerMessageSenderFactory {

  private final String kafkaClusterName;
  private final MessageSenderFactory messageSenderFactory;
  private final Trackers trackers;
  private final FutureAsyncTimeout futureAsyncTimeout;
  private final UndeliveredMessageLog undeliveredMessageLog;
  private final Clock clock;
  private final ConsumerAuthorizationHandler consumerAuthorizationHandler;
  private final ExecutorService rateLimiterReportingExecutor;
  private final int senderAsyncTimeoutMs;

  public ConsumerMessageSenderFactory(
      String kafkaClusterName,
      MessageSenderFactory messageSenderFactory,
      Trackers trackers,
      FutureAsyncTimeout futureAsyncTimeout,
      UndeliveredMessageLog undeliveredMessageLog,
      Clock clock,
      InstrumentedExecutorServiceFactory instrumentedExecutorServiceFactory,
      ConsumerAuthorizationHandler consumerAuthorizationHandler,
      int senderAsyncTimeoutMs,
      int rateLimiterReportingThreadPoolSize,
      boolean rateLimiterReportingThreadMonitoringEnabled) {

    this.kafkaClusterName = kafkaClusterName;
    this.messageSenderFactory = messageSenderFactory;
    this.trackers = trackers;
    this.futureAsyncTimeout = futureAsyncTimeout;
    this.undeliveredMessageLog = undeliveredMessageLog;
    this.clock = clock;
    this.consumerAuthorizationHandler = consumerAuthorizationHandler;
    this.rateLimiterReportingExecutor =
        instrumentedExecutorServiceFactory.getExecutorService(
            "rate-limiter-reporter",
            rateLimiterReportingThreadPoolSize,
            rateLimiterReportingThreadMonitoringEnabled);
    this.senderAsyncTimeoutMs = senderAsyncTimeoutMs;
  }

  public ConsumerMessageSender create(
      Subscription subscription,
      SerialConsumerRateLimiter consumerRateLimiter,
      PendingOffsets pendingOffsets,
      SubscriptionLoadRecorder subscriptionLoadRecorder,
      MetricsFacade metrics) {

    List<SuccessHandler> successHandlers =
        Arrays.asList(
            consumerAuthorizationHandler,
            new DefaultSuccessHandler(
                metrics,
                trackers,
                subscription.getQualifiedName(),
                subscription.getMetricsConfig()));

    List<ErrorHandler> errorHandlers =
        Arrays.asList(
            consumerAuthorizationHandler,
            new DefaultErrorHandler(
                metrics,
                undeliveredMessageLog,
                clock,
                trackers,
                kafkaClusterName,
                subscription.getQualifiedName()));

    return new ConsumerMessageSender(
        subscription,
        messageSenderFactory,
        successHandlers,
        errorHandlers,
        consumerRateLimiter,
        rateLimiterReportingExecutor,
        pendingOffsets,
        metrics,
        senderAsyncTimeoutMs,
        futureAsyncTimeout,
        clock,
        subscriptionLoadRecorder);
  }
}
