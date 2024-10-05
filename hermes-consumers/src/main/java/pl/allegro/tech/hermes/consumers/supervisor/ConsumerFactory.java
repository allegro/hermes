package pl.allegro.tech.hermes.consumers.supervisor;

import java.time.Clock;
import java.time.Duration;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.CommonConsumerParameters;
import pl.allegro.tech.hermes.consumers.consumer.BatchConsumer;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerAuthorizationHandler;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerMessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.SerialConsumer;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchFactory;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecordersRegistry;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimitSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculatorFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSenderFactory;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

public class ConsumerFactory {

  private final ConsumerRateLimitSupervisor consumerRateLimitSupervisor;
  private final OutputRateCalculatorFactory outputRateCalculatorFactory;
  private final ReceiverFactory messageReceiverFactory;
  private final MetricsFacade metrics;
  private final CommonConsumerParameters commonConsumerParameters;
  private final Trackers trackers;
  private final ConsumerMessageSenderFactory consumerMessageSenderFactory;
  private final TopicRepository topicRepository;
  private final MessageConverterResolver messageConverterResolver;
  private final MessageBatchFactory batchFactory;
  private final CompositeMessageContentWrapper compositeMessageContentWrapper;
  private final MessageBatchSenderFactory batchSenderFactory;
  private final ConsumerAuthorizationHandler consumerAuthorizationHandler;
  private final Clock clock;
  private final SubscriptionLoadRecordersRegistry subscriptionLoadRecordersRegistry;
  private final ConsumerPartitionAssignmentState consumerPartitionAssignmentState;
  private final Duration commitPeriod;
  private final int offsetQueueSize;

  public ConsumerFactory(
      ReceiverFactory messageReceiverFactory,
      MetricsFacade metrics,
      CommonConsumerParameters commonConsumerParameters,
      ConsumerRateLimitSupervisor consumerRateLimitSupervisor,
      OutputRateCalculatorFactory outputRateCalculatorFactory,
      Trackers trackers,
      ConsumerMessageSenderFactory consumerMessageSenderFactory,
      TopicRepository topicRepository,
      MessageConverterResolver messageConverterResolver,
      MessageBatchFactory byteBufferMessageBatchFactory,
      CompositeMessageContentWrapper compositeMessageContentWrapper,
      MessageBatchSenderFactory batchSenderFactory,
      ConsumerAuthorizationHandler consumerAuthorizationHandler,
      Clock clock,
      SubscriptionLoadRecordersRegistry subscriptionLoadRecordersRegistry,
      ConsumerPartitionAssignmentState consumerPartitionAssignmentState,
      Duration commitPeriod,
      int offsetQueueSize) {
    this.messageReceiverFactory = messageReceiverFactory;
    this.metrics = metrics;
    this.commonConsumerParameters = commonConsumerParameters;
    this.consumerRateLimitSupervisor = consumerRateLimitSupervisor;
    this.outputRateCalculatorFactory = outputRateCalculatorFactory;
    this.trackers = trackers;
    this.consumerMessageSenderFactory = consumerMessageSenderFactory;
    this.topicRepository = topicRepository;
    this.messageConverterResolver = messageConverterResolver;
    this.batchFactory = byteBufferMessageBatchFactory;
    this.compositeMessageContentWrapper = compositeMessageContentWrapper;
    this.batchSenderFactory = batchSenderFactory;
    this.consumerAuthorizationHandler = consumerAuthorizationHandler;
    this.clock = clock;
    this.subscriptionLoadRecordersRegistry = subscriptionLoadRecordersRegistry;
    this.consumerPartitionAssignmentState = consumerPartitionAssignmentState;
    this.commitPeriod = commitPeriod;
    this.offsetQueueSize = offsetQueueSize;
  }

  public Consumer createConsumer(Subscription subscription) {
    Topic topic = topicRepository.getTopicDetails(subscription.getTopicName());
    SubscriptionLoadRecorder loadRecorder =
        subscriptionLoadRecordersRegistry.register(subscription.getQualifiedName());
    if (subscription.isBatchSubscription()) {
      return new BatchConsumer(
          messageReceiverFactory,
          batchSenderFactory.create(subscription),
          batchFactory,
          messageConverterResolver,
          compositeMessageContentWrapper,
          metrics,
          trackers,
          subscription,
          topic,
          commonConsumerParameters.isUseTopicMessageSizeEnabled(),
          loadRecorder,
          commitPeriod);
    } else {
      SerialConsumerRateLimiter consumerRateLimiter =
          new SerialConsumerRateLimiter(
              subscription,
              outputRateCalculatorFactory,
              metrics,
              consumerRateLimitSupervisor,
              clock);

      return new SerialConsumer(
          messageReceiverFactory,
          metrics,
          subscription,
          consumerRateLimiter,
          consumerMessageSenderFactory,
          trackers,
          messageConverterResolver,
          topic,
          commonConsumerParameters,
          consumerAuthorizationHandler,
          loadRecorder,
          consumerPartitionAssignmentState,
          commitPeriod,
          offsetQueueSize);
    }
  }
}
