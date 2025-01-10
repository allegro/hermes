package pl.allegro.tech.hermes.consumers.consumer;

import static com.github.rholder.retry.WaitStrategies.fixedWait;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.BatchSubscriptionPolicy;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchFactory;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchReceiver;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchingResult;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartition;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.rate.BatchConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

public class BatchConsumer implements Consumer {

  private static final Logger logger = LoggerFactory.getLogger(BatchConsumer.class);

  private final ReceiverFactory messageReceiverFactory;
  private final MessageBatchSender sender;
  private final MessageBatchFactory batchFactory;
  private final boolean useTopicMessageSize;
  private final MessageConverterResolver messageConverterResolver;
  private final CompositeMessageContentWrapper compositeMessageContentWrapper;
  private final Trackers trackers;
  private final SubscriptionLoadRecorder loadRecorder;
  private final Duration commitPeriod;

  private Topic topic;
  private Subscription subscription;

  private volatile boolean consuming = true;

  private final MetricsFacade metricsFacade;
  private final BatchConsumerMetrics metrics;
  private MessageBatchReceiver receiver;

  private final Map<SubscriptionPartition, Long> maxPendingOffsets = new HashMap<>();

  private Instant lastCommitTime;

  public BatchConsumer(
      ReceiverFactory messageReceiverFactory,
      MessageBatchSender sender,
      MessageBatchFactory batchFactory,
      MessageConverterResolver messageConverterResolver,
      CompositeMessageContentWrapper compositeMessageContentWrapper,
      MetricsFacade metricsFacade,
      Trackers trackers,
      Subscription subscription,
      Topic topic,
      boolean useTopicMessageSize,
      SubscriptionLoadRecorder loadRecorder,
      Duration commitPeriod) {
    this.messageReceiverFactory = messageReceiverFactory;
    this.sender = sender;
    this.batchFactory = batchFactory;
    this.subscription = subscription;
    this.useTopicMessageSize = useTopicMessageSize;
    this.loadRecorder = loadRecorder;
    this.metricsFacade = metricsFacade;
    this.metrics = new BatchConsumerMetrics(metricsFacade, subscription.getQualifiedName());
    this.messageConverterResolver = messageConverterResolver;
    this.compositeMessageContentWrapper = compositeMessageContentWrapper;
    this.topic = topic;
    this.trackers = trackers;
    this.commitPeriod = commitPeriod;
    this.lastCommitTime = Instant.now();
  }

  @Override
  public void consume(Runnable signalsInterrupt) {
    Optional<MessageBatch> inflight = Optional.empty();
    try {
      logger.debug(
          "Trying to create new batch [subscription={}].", subscription.getQualifiedName());

      signalsInterrupt.run();
      commitIfReady();

      MessageBatchingResult result = receiver.next(subscription, signalsInterrupt);
      inflight = of(result.getBatch());

      inflight.ifPresent(
          batch -> {
            logger.debug("Delivering batch [subscription={}].", subscription.getQualifiedName());

            deliver(
                signalsInterrupt,
                batch,
                createRetryer(batch, subscription.getBatchSubscriptionPolicy()));

            offerProcessedOffsets(batch);
            logger.debug(
                "Finished delivering batch [subscription={}]", subscription.getQualifiedName());
          });

      result
          .getDiscarded()
          .forEach(
              m -> {
                metrics.markDiscarded();
                trackers.get(subscription).logDiscarded(m, "too large");
              });
    } finally {
      logger.debug("Cleaning batch [subscription={}]", subscription.getQualifiedName());
      inflight.ifPresent(this::clean);
    }
  }

  private void commitIfReady() {
    if (isReadyToCommit()) {
      Set<SubscriptionPartitionOffset> offsetsToCommit = new HashSet<>();

      for (Map.Entry<SubscriptionPartition, Long> entry : maxPendingOffsets.entrySet()) {
        offsetsToCommit.add(new SubscriptionPartitionOffset(entry.getKey(), entry.getValue()));
      }

      if (!offsetsToCommit.isEmpty()) {
        commit(offsetsToCommit);
      }
      maxPendingOffsets.clear();
      lastCommitTime = Instant.now();
    }
  }

  private boolean isReadyToCommit() {
    return Duration.between(lastCommitTime, Instant.now()).toMillis() > commitPeriod.toMillis();
  }

  private void offerProcessedOffsets(MessageBatch batch) {
    for (SubscriptionPartitionOffset offset : batch.getPartitionOffsets()) {
      putOffset(offset);
    }
  }

  private void putOffset(SubscriptionPartitionOffset offset) {
    maxPendingOffsets.compute(
        offset.getSubscriptionPartition(),
        (subscriptionPartition, maxOffset) ->
            maxOffset == null ? offset.getOffset() : Math.max(maxOffset, offset.getOffset()));
  }

  @Override
  public void initialize() {
    loadRecorder.initialize();
    logger.debug(
        "Consumer: preparing receiver for subscription {}", subscription.getQualifiedName());
    MessageReceiver receiver =
        messageReceiverFactory.createMessageReceiver(
            topic,
            subscription,
            new BatchConsumerRateLimiter(),
            loadRecorder,
            metricsFacade,
            this::putOffset);

    logger.debug(
        "Consumer: preparing batch receiver for subscription {}", subscription.getQualifiedName());
    this.receiver =
        new MessageBatchReceiver(
            receiver,
            batchFactory,
            messageConverterResolver,
            compositeMessageContentWrapper,
            topic,
            trackers,
            loadRecorder,
            this::commitIfReady);
    metrics.initialize();
  }

  @Override
  public void tearDown() {
    consuming = false;
    if (receiver != null) {
      receiver.stop();
    } else {
      logger.info("No batch receiver to stop [subscription={}].", subscription.getQualifiedName());
    }
    loadRecorder.shutdown();
    metrics.shutdown();
  }

  @Override
  public void updateSubscription(Subscription subscription) {
    this.subscription = subscription;
    receiver.updateSubscription(subscription);
  }

  @Override
  public void updateTopic(Topic newTopic) {
    if (this.topic.getContentType() != newTopic.getContentType() || messageSizeChanged(newTopic)) {
      logger.info("Reinitializing message receiver, contentType or messageSize changed.");
      this.topic = newTopic;
      tearDown();
      initialize();
    }
  }

  private boolean messageSizeChanged(Topic newTopic) {
    return this.topic.getMaxMessageSize() != newTopic.getMaxMessageSize() && useTopicMessageSize;
  }

  @Override
  public void commit(Set<SubscriptionPartitionOffset> offsetsToCommit) {
    if (receiver != null) {
      receiver.commit(offsetsToCommit);
    }
  }

  @Override
  public PartitionOffsets moveOffset(PartitionOffsets partitionOffsets) {
    if (receiver != null) {
      return receiver.moveOffset(partitionOffsets);
    }
    return new PartitionOffsets();
  }

  @Override
  public Subscription getSubscription() {
    return subscription;
  }

  @Override
  public Set<Integer> getAssignedPartitions() {
    return receiver.getAssignedPartitions();
  }

  private Retryer<MessageSendingResult> createRetryer(
      MessageBatch batch, BatchSubscriptionPolicy policy) {
    return createRetryer(
        batch,
        policy.getMessageBackoff(),
        SECONDS.toMillis(policy.getMessageTtl()),
        policy.isRetryClientErrors());
  }

  private Retryer<MessageSendingResult> createRetryer(
      final MessageBatch batch,
      int messageBackoff,
      long messageTtlMillis,
      boolean retryClientErrors) {
    return RetryerBuilder.<MessageSendingResult>newBuilder()
        .retryIfExceptionOfType(IOException.class)
        .retryIfRuntimeException()
        .retryIfResult(
            result ->
                consuming
                    && !result.succeeded()
                    && shouldRetryOnClientError(retryClientErrors, result))
        .withWaitStrategy(fixedWait(messageBackoff, MILLISECONDS))
        .withStopStrategy(
            attempt ->
                attempt.getDelaySinceFirstAttempt() > messageTtlMillis
                    || Thread.currentThread().isInterrupted())
        .withRetryListener(
            getRetryListener(
                result -> {
                  batch.incrementRetryCounter();
                  markSendingResult(batch, result);
                }))
        .build();
  }

  private void markSendingResult(MessageBatch batch, MessageSendingResult result) {
    if (result.succeeded()) {
      metrics.recordAttemptAsFinished(batch.getMessageCount());
      metrics.markSuccess(batch, result);
      batch
          .getMessagesMetadata()
          .forEach(m -> trackers.get(subscription).logSent(m, result.getHostname()));
    } else {
      metrics.markFailure(batch, result);
      batch
          .getMessagesMetadata()
          .forEach(
              m ->
                  trackers
                      .get(subscription)
                      .logFailed(m, result.getRootCause(), result.getHostname()));
    }
  }

  private boolean shouldRetryOnClientError(boolean retryClientErrors, MessageSendingResult result) {
    return !result.isClientError() || retryClientErrors;
  }

  private void deliver(
      Runnable signalsInterrupt, MessageBatch batch, Retryer<MessageSendingResult> retryer) {
    metrics.recordAttempt(batch.getMessageCount());
    try (HermesTimerContext ignored = metrics.latencyTimer().time()) {
      retryer.call(
          () -> {
            loadRecorder.recordSingleOperation();
            signalsInterrupt.run();
            return sender.send(
                batch,
                subscription.getEndpoint(),
                subscription.getEndpointAddressResolverMetadata(),
                subscription.getBatchSubscriptionPolicy().getRequestTimeout());
          });
    } catch (Exception e) {
      logger.error(
          "Batch was rejected [batch_id={}, subscription={}].",
          batch.getId(),
          subscription.getQualifiedName(),
          e);
      metrics.recordAttemptAsFinished(batch.getMessageCount());
      metrics.markDiscarded(batch);
      batch
          .getMessagesMetadata()
          .forEach(m -> trackers.get(subscription).logDiscarded(m, e.getMessage()));
    }
  }

  private void clean(MessageBatch batch) {
    batchFactory.destroyBatch(batch);
  }

  private RetryListener getRetryListener(
      java.util.function.Consumer<MessageSendingResult> consumer) {
    return new RetryListener() {
      @Override
      public <V> void onRetry(Attempt<V> attempt) {
        if (attempt.hasException()) {
          consumer.accept(MessageSendingResult.failedResult(attempt.getExceptionCause()));
        } else {
          consumer.accept((MessageSendingResult) attempt.getResult());
        }
      }
    };
  }
}
