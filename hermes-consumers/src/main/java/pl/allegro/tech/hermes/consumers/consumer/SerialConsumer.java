package pl.allegro.tech.hermes.consumers.consumer;

import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.CommonConsumerParameters;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetCommitter;
import pl.allegro.tech.hermes.consumers.consumer.offset.PendingOffsets;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.profiling.ConsumerProfiler;
import pl.allegro.tech.hermes.consumers.consumer.profiling.ConsumerRun;
import pl.allegro.tech.hermes.consumers.consumer.profiling.DefaultConsumerProfiler;
import pl.allegro.tech.hermes.consumers.consumer.profiling.Measurement;
import pl.allegro.tech.hermes.consumers.consumer.profiling.NoOpConsumerProfiler;
import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.UninitializedMessageReceiver;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

public class SerialConsumer implements Consumer {

  private static final Logger logger = LoggerFactory.getLogger(SerialConsumer.class);

  private final ReceiverFactory messageReceiverFactory;
  private final MetricsFacade metrics;
  private final SerialConsumerRateLimiter rateLimiter;
  private final Trackers trackers;
  private final MessageConverterResolver messageConverterResolver;
  private final ConsumerMessageSender sender;
  private final boolean useTopicMessageSizeEnabled;
  private final PendingOffsets pendingOffsets;
  private final ConsumerAuthorizationHandler consumerAuthorizationHandler;
  private final SubscriptionLoadRecorder loadRecorder;
  private final OffsetCommitter offsetCommitter;
  private final Duration commitPeriod;

  private final int defaultInflight;
  private final Duration signalProcessingInterval;

  private Topic topic;
  private Subscription subscription;

  private MessageReceiver messageReceiver;

  private Instant lastCommitTime;

  public SerialConsumer(
      ReceiverFactory messageReceiverFactory,
      MetricsFacade metrics,
      Subscription subscription,
      SerialConsumerRateLimiter rateLimiter,
      ConsumerMessageSenderFactory consumerMessageSenderFactory,
      Trackers trackers,
      MessageConverterResolver messageConverterResolver,
      Topic topic,
      CommonConsumerParameters commonConsumerParameters,
      ConsumerAuthorizationHandler consumerAuthorizationHandler,
      SubscriptionLoadRecorder loadRecorder,
      ConsumerPartitionAssignmentState consumerPartitionAssignmentState,
      Duration commitPeriod,
      int offsetQueueSize) {
    this.defaultInflight = commonConsumerParameters.getSerialConsumer().getInflightSize();
    this.signalProcessingInterval =
        commonConsumerParameters.getSerialConsumer().getSignalProcessingInterval();
    this.messageReceiverFactory = messageReceiverFactory;
    this.metrics = metrics;
    this.subscription = subscription;
    this.rateLimiter = rateLimiter;
    this.useTopicMessageSizeEnabled = commonConsumerParameters.isUseTopicMessageSizeEnabled();
    this.pendingOffsets =
        new PendingOffsets(
            subscription.getQualifiedName(),
            metrics,
            calculateInflightSize(subscription),
            offsetQueueSize);
    this.consumerAuthorizationHandler = consumerAuthorizationHandler;
    this.trackers = trackers;
    this.messageConverterResolver = messageConverterResolver;
    this.loadRecorder = loadRecorder;
    this.messageReceiver = new UninitializedMessageReceiver();
    this.topic = topic;
    this.offsetCommitter = new OffsetCommitter(consumerPartitionAssignmentState, metrics);
    this.sender =
        consumerMessageSenderFactory.create(
            subscription, rateLimiter, pendingOffsets, loadRecorder, metrics);
    this.commitPeriod = commitPeriod;
    this.lastCommitTime = Instant.now();
  }

  private int calculateInflightSize(Subscription subscription) {
    Optional<Integer> subscriptionInflight =
        Optional.ofNullable(subscription.getSerialSubscriptionPolicy().getInflightSize());
    return subscriptionInflight.orElse(defaultInflight);
  }

  @Override
  public void consume(Runnable signalsInterrupt) {
    try {
      ConsumerProfiler profiler =
          subscription.isProfilingEnabled()
              ? new DefaultConsumerProfiler(
                  subscription.getQualifiedName(), subscription.getProfilingThresholdMs())
              : new NoOpConsumerProfiler();
      profiler.startMeasurements(Measurement.SIGNALS_AND_SEMAPHORE_ACQUIRE);
      do {
        loadRecorder.recordSingleOperation();
        profiler.startPartialMeasurement(Measurement.SIGNALS_INTERRUPT_RUN);
        signalsInterrupt.run();
        commitIfReady();
        profiler.stopPartialMeasurement();
      } while (!pendingOffsets.tryAcquireSlot(signalProcessingInterval));

      profiler.measure(Measurement.MESSAGE_RECEIVER_NEXT);
      Optional<Message> maybeMessage = messageReceiver.next();

      profiler.measure(Measurement.MESSAGE_CONVERSION);
      if (maybeMessage.isPresent()) {
        Message message = maybeMessage.get();

        if (message.isFiltered()) {
          profiler.flushMeasurements(ConsumerRun.FILTERED);
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug(
                "Read message {} partition {} offset {}",
                message.getContentType(),
                message.getPartition(),
                message.getOffset());
          }

          Message convertedMessage =
              messageConverterResolver.converterFor(message, subscription).convert(message, topic);
          sendMessage(convertedMessage, profiler);
        }
      } else {
        pendingOffsets.releaseSlot();
        profiler.flushMeasurements(ConsumerRun.EMPTY);
      }
    } catch (InterruptedException e) {
      logger.info("Restoring interrupted status {}", subscription.getQualifiedName(), e);
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      logger.error("Consumer loop failed for {}", subscription.getQualifiedName(), e);
    }
  }

  private void commitIfReady() {
    if (isReadyToCommit()) {
      Set<SubscriptionPartitionOffset> offsetsToCommit =
          offsetCommitter.calculateOffsetsToBeCommitted(
              pendingOffsets.getOffsetsSnapshotAndReleaseProcessedSlots());
      if (!offsetsToCommit.isEmpty()) {
        commit(offsetsToCommit);
      }
      lastCommitTime = Instant.now();
    }
  }

  private boolean isReadyToCommit() {
    return Duration.between(lastCommitTime, Instant.now()).toMillis() > commitPeriod.toMillis();
  }

  private void sendMessage(Message message, ConsumerProfiler profiler) throws InterruptedException {
    profiler.measure(Measurement.OFFER_INFLIGHT_OFFSET);
    pendingOffsets.markAsInflight(
        subscriptionPartitionOffset(
            subscription.getQualifiedName(),
            message.getPartitionOffset(),
            message.getPartitionAssignmentTerm()));

    profiler.measure(Measurement.TRACKERS_LOG_INFLIGHT);
    trackers.get(subscription).logInflight(toMessageMetadata(message, subscription));

    sender.sendAsync(message, profiler);
  }

  @Override
  public void initialize() {
    logger.info(
        "Consumer: preparing message receiver for subscription {}",
        subscription.getQualifiedName());
    initializeMessageReceiver();
    sender.initialize();
    rateLimiter.initialize();
    loadRecorder.initialize();
    consumerAuthorizationHandler.createSubscriptionHandler(subscription.getQualifiedName());
  }

  private void initializeMessageReceiver() {
    this.messageReceiver =
        messageReceiverFactory.createMessageReceiver(
            topic,
            subscription,
            rateLimiter,
            loadRecorder,
            metrics,
            pendingOffsets::markAsProcessed);
  }

  /**
   * Try to keep shutdown order the same as initialization so nothing will left to clean up when
   * error occurs during initialization.
   */
  @Override
  public void tearDown() {
    messageReceiver.stop();
    sender.shutdown();
    rateLimiter.shutdown();
    loadRecorder.shutdown();
    consumerAuthorizationHandler.removeSubscriptionHandler(subscription.getQualifiedName());
    metrics.unregisterAllMetricsRelatedTo(subscription.getQualifiedName());
  }

  @Override
  public void updateSubscription(Subscription newSubscription) {
    logger.info("Updating consumer for subscription {}", subscription.getQualifiedName());
    pendingOffsets.setInflightSize(calculateInflightSize(newSubscription));
    rateLimiter.updateSubscription(newSubscription);
    sender.updateSubscription(newSubscription);
    messageReceiver.update(newSubscription);
    consumerAuthorizationHandler.updateSubscription(newSubscription.getQualifiedName());
    this.subscription = newSubscription;
  }

  @Override
  public void updateTopic(Topic newTopic) {
    if (this.topic.getContentType() != newTopic.getContentType()
        || messageSizeChanged(newTopic)
        || this.topic.isSchemaIdAwareSerializationEnabled()
            != newTopic.isSchemaIdAwareSerializationEnabled()) {
      logger.info(
          "Reinitializing message receiver, contentType, messageSize or schemaIdAwareSerialization changed.");
      this.topic = newTopic;

      messageReceiver.stop();
      initializeMessageReceiver();
    }
  }

  private boolean messageSizeChanged(Topic newTopic) {
    return this.topic.getMaxMessageSize() != newTopic.getMaxMessageSize()
        && useTopicMessageSizeEnabled;
  }

  @Override
  public void commit(Set<SubscriptionPartitionOffset> offsets) {
    messageReceiver.commit(offsets);
  }

  @Override
  public boolean moveOffset(PartitionOffset offset) {
    return messageReceiver.moveOffset(offset);
  }

  @Override
  public Subscription getSubscription() {
    return subscription;
  }
}
