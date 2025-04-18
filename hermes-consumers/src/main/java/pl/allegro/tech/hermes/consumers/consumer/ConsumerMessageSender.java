package pl.allegro.tech.hermes.consumers.consumer;

import static java.lang.String.format;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.consumer.offset.PendingOffsets;
import pl.allegro.tech.hermes.consumers.consumer.profiling.ConsumerProfiler;
import pl.allegro.tech.hermes.consumers.consumer.profiling.ConsumerRun;
import pl.allegro.tech.hermes.consumers.consumer.profiling.DefaultConsumerProfiler;
import pl.allegro.tech.hermes.consumers.consumer.profiling.Measurement;
import pl.allegro.tech.hermes.consumers.consumer.profiling.NoOpConsumerProfiler;
import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SubscriptionChangeListener;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResultLogInfo;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;

public class ConsumerMessageSender {

  private static final Logger logger = LoggerFactory.getLogger(ConsumerMessageSender.class);
  private final ExecutorService deliveryReportingExecutor;
  private final List<SuccessHandler> successHandlers;
  private final List<ErrorHandler> errorHandlers;
  private final MessageSenderFactory messageSenderFactory;
  private final Clock clock;
  private final PendingOffsets pendingOffsets;
  private final SubscriptionLoadRecorder loadRecorder;
  private final HermesTimer consumerLatencyTimer;
  private final HermesCounter retries;
  private final SerialConsumerRateLimiter rateLimiter;
  private final HermesTimer rateLimiterAcquireTimer;
  private final FutureAsyncTimeout async;
  private final int asyncTimeoutMs;
  private final LongAdder inflightCount = new LongAdder();

  private MessageSender messageSender;
  private Subscription subscription;

  private ScheduledExecutorService retrySingleThreadExecutor;
  private volatile boolean running = true;

  public ConsumerMessageSender(
      Subscription subscription,
      MessageSenderFactory messageSenderFactory,
      List<SuccessHandler> successHandlers,
      List<ErrorHandler> errorHandlers,
      SerialConsumerRateLimiter rateLimiter,
      ExecutorService deliveryReportingExecutor,
      PendingOffsets pendingOffsets,
      MetricsFacade metrics,
      int asyncTimeoutMs,
      FutureAsyncTimeout futureAsyncTimeout,
      Clock clock,
      SubscriptionLoadRecorder loadRecorder) {
    this.deliveryReportingExecutor = deliveryReportingExecutor;
    this.successHandlers = successHandlers;
    this.errorHandlers = errorHandlers;
    this.messageSenderFactory = messageSenderFactory;
    this.clock = clock;
    this.loadRecorder = loadRecorder;
    this.async = futureAsyncTimeout;
    this.rateLimiter = rateLimiter;
    this.asyncTimeoutMs = asyncTimeoutMs;
    this.messageSender = messageSender(subscription);
    this.subscription = subscription;
    this.pendingOffsets = pendingOffsets;
    this.consumerLatencyTimer = metrics.subscriptions().latency(subscription.getQualifiedName());
    metrics
        .subscriptions()
        .registerInflightGauge(
            subscription.getQualifiedName(), this, sender -> sender.inflightCount.doubleValue());
    this.retries = metrics.subscriptions().retries(subscription.getQualifiedName());
    this.rateLimiterAcquireTimer =
        metrics.subscriptions().rateLimiterAcquire(subscription.getQualifiedName());
  }

  public void initialize() {
    running = true;
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder()
            .setNameFormat(subscription.getQualifiedName() + "-retry-executor-%d")
            .build();
    this.retrySingleThreadExecutor = Executors.newScheduledThreadPool(1, threadFactory);
  }

  public void shutdown() {
    running = false;
    messageSender.stop();
    retrySingleThreadExecutor.shutdownNow();
    try {
      retrySingleThreadExecutor.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      logger.warn("Failed to stop retry executor within one minute with following exception", e);
    }
  }

  public void sendAsync(Message message, ConsumerProfiler profiler) {
    inflightCount.increment();
    sendAsync(message, calculateMessageDelay(message.getPublishingTimestamp()), profiler);
  }

  private void sendAsync(Message message, int delayMillis, ConsumerProfiler profiler) {
    profiler.measure(Measurement.SCHEDULE_MESSAGE_SENDING);
    retrySingleThreadExecutor.schedule(
        () -> sendMessage(message, profiler), delayMillis, TimeUnit.MILLISECONDS);
  }

  private int calculateMessageDelay(long publishingMessageTimestamp) {
    Integer delay = subscription.getSerialSubscriptionPolicy().getSendingDelay();
    if (INTEGER_ZERO.equals(delay)) {
      return delay;
    }

    long messageAgeAtThisPoint = clock.millis() - publishingMessageTimestamp;

    delay = delay - (int) messageAgeAtThisPoint;

    return Math.max(delay, INTEGER_ZERO);
  }

  /**
   * Method is calling MessageSender and is registering listeners to handle response. Main
   * responsibility of this method is that no message will be fully processed or rejected without
   * release on semaphore.
   */
  private void sendMessage(final Message message, ConsumerProfiler profiler) {
    loadRecorder.recordSingleOperation();
    profiler.measure(Measurement.ACQUIRE_RATE_LIMITER);
    acquireRateLimiterWithTimer();
    HermesTimerContext timer = consumerLatencyTimer.time();
    profiler.measure(Measurement.MESSAGE_SENDER_SEND);
    CompletableFuture<MessageSendingResult> response = messageSender.send(message);

    response
        .thenAcceptAsync(
            new ResponseHandlingListener(message, timer, profiler), deliveryReportingExecutor)
        .exceptionally(
            e -> {
              logger.error(
                  "An error occurred while handling message sending response of subscription {} [partition={}, offset={}, id={}]",
                  subscription.getQualifiedName(),
                  message.getPartition(),
                  message.getOffset(),
                  message.getId(),
                  e);
              return null;
            });
  }

  private void acquireRateLimiterWithTimer() {
    HermesTimerContext acquireTimer = rateLimiterAcquireTimer.time();
    rateLimiter.acquire();
    acquireTimer.close();
  }

  private MessageSender messageSender(Subscription subscription) {
    Integer requestTimeoutMs = subscription.getSerialSubscriptionPolicy().getRequestTimeout();
    ResilientMessageSender resilientMessageSender =
        new ResilientMessageSender(
            this.rateLimiter, subscription, this.async, requestTimeoutMs, this.asyncTimeoutMs);

    return this.messageSenderFactory.create(subscription, resilientMessageSender);
  }

  public void updateSubscription(Subscription newSubscription) {
    boolean endpointUpdated =
        !this.subscription.getEndpoint().equals(newSubscription.getEndpoint());
    boolean subscriptionPolicyUpdated =
        !Objects.equals(
            this.subscription.getSerialSubscriptionPolicy(),
            newSubscription.getSerialSubscriptionPolicy());
    boolean endpointAddressResolverMetadataChanged =
        !Objects.equals(
            this.subscription.getEndpointAddressResolverMetadata(),
            newSubscription.getEndpointAddressResolverMetadata());
    boolean oAuthPolicyChanged =
        !Objects.equals(this.subscription.getOAuthPolicy(), newSubscription.getOAuthPolicy());
    boolean subscriptionMetricsConfigChanged =
        !Objects.equals(this.subscription.getMetricsConfig(), newSubscription.getMetricsConfig());

    this.subscription = newSubscription;

    boolean httpClientChanged =
        this.subscription.isHttp2Enabled() != newSubscription.isHttp2Enabled();

    if (endpointUpdated
        || subscriptionPolicyUpdated
        || endpointAddressResolverMetadataChanged
        || oAuthPolicyChanged
        || httpClientChanged) {
      this.messageSender.stop();
      this.messageSender = messageSender(newSubscription);
    }
    if (subscriptionMetricsConfigChanged) {
      this.successHandlers.stream()
          .filter(SubscriptionChangeListener.class::isInstance)
          .map(SubscriptionChangeListener.class::cast)
          .forEach(successHandler -> successHandler.updateSubscription(newSubscription));
    }
  }

  private boolean willExceedTtl(Message message, long delay) {
    long ttl =
        TimeUnit.SECONDS.toMillis(subscription.getSerialSubscriptionPolicy().getMessageTtl());
    long remainingTtl = Math.max(ttl - delay, 0);
    return message.isTtlExceeded(remainingTtl);
  }

  private void handleFailedSending(
      Message message, MessageSendingResult result, ConsumerProfiler profiler) {
    errorHandlers.forEach(h -> h.handleFailed(message, subscription, result));
    retrySendingOrDiscard(message, result, profiler);
  }

  private void retrySendingOrDiscard(
      Message message, MessageSendingResult result, ConsumerProfiler profiler) {
    List<URI> succeededUris =
        result.getSucceededUris(ConsumerMessageSender.this::messageSentSucceeded);
    message.incrementRetryCounter(succeededUris);

    long retryDelay = extractRetryDelay(message, result);
    if (shouldAttemptResending(message, result, retryDelay)) {
      retries.increment();
      profiler.flushMeasurements(ConsumerRun.RETRIED);
      ConsumerProfiler resendProfiler =
          subscription.isProfilingEnabled()
              ? new DefaultConsumerProfiler(
                  subscription.getQualifiedName(), subscription.getProfilingThresholdMs())
              : new NoOpConsumerProfiler();
      resendProfiler.startMeasurements(Measurement.SCHEDULE_RESEND);
      resendProfiler.saveRetryDelay(retryDelay);
      retrySingleThreadExecutor.schedule(
          () -> resend(message, result, resendProfiler), retryDelay, TimeUnit.MILLISECONDS);
    } else {
      handleMessageDiscarding(message, result, profiler);
    }
  }

  private boolean shouldAttemptResending(
      Message message, MessageSendingResult result, long retryDelay) {
    return !willExceedTtl(message, retryDelay) && shouldResendMessage(result);
  }

  private long extractRetryDelay(Message message, MessageSendingResult result) {
    long defaultBackoff =
        message.updateAndGetCurrentMessageBackoff(subscription.getSerialSubscriptionPolicy());
    long ttl =
        TimeUnit.SECONDS.toMillis(subscription.getSerialSubscriptionPolicy().getMessageTtl());
    return result.getRetryAfterMillis().map(delay -> Math.min(delay, ttl)).orElse(defaultBackoff);
  }

  private void resend(Message message, MessageSendingResult result, ConsumerProfiler profiler) {
    if (result.isLoggable()) {
      result.getLogInfo().forEach(logInfo -> logResultInfo(message, logInfo));
    }
    sendMessage(message, profiler);
  }

  private void logResultInfo(Message message, MessageSendingResultLogInfo logInfo) {
    logger.debug(
        format(
            "Retrying message send to endpoint %s; messageId %s; offset: %s; partition: %s; sub id: %s; rootCause: %s",
            logInfo.getUrlString(),
            message.getId(),
            message.getOffset(),
            message.getPartition(),
            subscription.getQualifiedName(),
            logInfo.getRootCause()),
        logInfo.getFailure());
  }

  private void handleMessageDiscarding(
      Message message, MessageSendingResult result, ConsumerProfiler profiler) {
    pendingOffsets.markAsProcessed(
        subscriptionPartitionOffset(
            subscription.getQualifiedName(),
            message.getPartitionOffset(),
            message.getPartitionAssignmentTerm()));
    inflightCount.decrement();
    errorHandlers.forEach(h -> h.handleDiscarded(message, subscription, result));
    profiler.flushMeasurements(ConsumerRun.DISCARDED);
  }

  private void handleMessageSendingSuccess(
      Message message, MessageSendingResult result, ConsumerProfiler profiler) {
    pendingOffsets.markAsProcessed(
        subscriptionPartitionOffset(
            subscription.getQualifiedName(),
            message.getPartitionOffset(),
            message.getPartitionAssignmentTerm()));
    inflightCount.decrement();
    successHandlers.forEach(h -> h.handleSuccess(message, subscription, result));
    profiler.flushMeasurements(ConsumerRun.DELIVERED);
  }

  private boolean messageSentSucceeded(MessageSendingResult result) {
    return result.succeeded() || (result.isClientError() && !shouldRetryOnClientError());
  }

  private boolean shouldResendMessage(MessageSendingResult result) {
    return !result.succeeded()
        && (!result.isClientError()
            || shouldRetryOnClientError()
            || isUnauthorizedForOAuthSecuredSubscription(result));
  }

  private boolean shouldRetryOnClientError() {
    return subscription.getSerialSubscriptionPolicy().isRetryClientErrors();
  }

  private boolean isUnauthorizedForOAuthSecuredSubscription(MessageSendingResult result) {
    return subscription.hasOAuthPolicy() && result.getStatusCode() == HttpStatus.UNAUTHORIZED_401;
  }

  class ResponseHandlingListener implements java.util.function.Consumer<MessageSendingResult> {

    private final Message message;
    private final HermesTimerContext timer;
    private final ConsumerProfiler profiler;

    public ResponseHandlingListener(
        Message message, HermesTimerContext timer, ConsumerProfiler profiler) {
      this.message = message;
      this.timer = timer;
      this.profiler = profiler;
    }

    @Override
    public void accept(MessageSendingResult result) {
      timer.close();
      loadRecorder.recordSingleOperation();
      profiler.measure(Measurement.HANDLERS);
      if (running) {
        if (result.succeeded()) {
          handleMessageSendingSuccess(message, result, profiler);
        } else {
          handleFailedSending(message, result, profiler);
        }
      } else {
        logger.warn(
            "Process of subscription {} is not running. "
                + "Ignoring sending message result [successful={}, partition={}, offset={}, id={}]",
            subscription.getQualifiedName(),
            result.succeeded(),
            message.getPartition(),
            message.getOffset(),
            message.getId());
      }
    }
  }
}
