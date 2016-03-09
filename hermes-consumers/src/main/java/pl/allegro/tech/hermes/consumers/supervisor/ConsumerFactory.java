package pl.allegro.tech.hermes.consumers.supervisor;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.BatchConsumer;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.SerialConsumer;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerMessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchFactory;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimitSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculator;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSenderFactory;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import javax.inject.Inject;
import java.time.Clock;
import java.util.concurrent.Semaphore;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_INFLIGHT_SIZE;

public class ConsumerFactory {

    private final ConsumerRateLimitSupervisor consumerRateLimitSupervisor;
    private final OutputRateCalculator outputRateCalculator;
    private final ReceiverFactory messageReceiverFactory;
    private final HermesMetrics hermesMetrics;
    private final ConfigFactory configFactory;
    private final Trackers trackers;
    private final ConsumerMessageSenderFactory consumerMessageSenderFactory;
    private final Clock clock;
    private final TopicRepository topicRepository;
    private final MessageConverterResolver messageConverterResolver;
    private final MessageBatchFactory batchFactory;
    private final MessageContentWrapper messageContentWrapper;
    private final MessageBatchSenderFactory batchSenderFactory;

    @Inject
    public ConsumerFactory(ReceiverFactory messageReceiverFactory,
                           HermesMetrics hermesMetrics,
                           ConfigFactory configFactory,
                           ConsumerRateLimitSupervisor consumerRateLimitSupervisor,
                           OutputRateCalculator outputRateCalculator,
                           Trackers trackers,
                           ConsumerMessageSenderFactory consumerMessageSenderFactory,
                           Clock clock,
                           TopicRepository topicRepository,
                           MessageConverterResolver messageConverterResolver,
                           MessageBatchFactory byteBufferMessageBatchFactory,
                           MessageContentWrapper messageContentWrapper,
                           MessageBatchSenderFactory batchSenderFactory) {

        this.messageReceiverFactory = messageReceiverFactory;
        this.hermesMetrics = hermesMetrics;
        this.configFactory = configFactory;
        this.consumerRateLimitSupervisor = consumerRateLimitSupervisor;
        this.outputRateCalculator = outputRateCalculator;
        this.trackers = trackers;
        this.consumerMessageSenderFactory = consumerMessageSenderFactory;
        this.clock = clock;
        this.topicRepository = topicRepository;
        this.messageConverterResolver = messageConverterResolver;
        this.batchFactory = byteBufferMessageBatchFactory;
        this.messageContentWrapper = messageContentWrapper;
        this.batchSenderFactory = batchSenderFactory;
    }

    Consumer createConsumer(Subscription subscription) {
        SubscriptionOffsetCommitQueues subscriptionOffsetCommitQueues = new SubscriptionOffsetCommitQueues(
                subscription, hermesMetrics, clock, configFactory);

        Topic topic = topicRepository.getTopicDetails(subscription.getTopicName());
        MessageReceiver messageReceiver = messageReceiverFactory.createMessageReceiver(topic, subscription);

        if (subscription.isBatchSubscription()) {
            return new BatchConsumer(messageReceiver,
                    batchSenderFactory.create(subscription),
                    batchFactory,
                    subscriptionOffsetCommitQueues,
                    messageConverterResolver,
                    messageContentWrapper,
                    hermesMetrics,
                    trackers,
                    subscription,
                    topic);
        } else {
            Semaphore inflightSemaphore = new Semaphore(configFactory.getIntProperty(CONSUMER_INFLIGHT_SIZE));

            ConsumerRateLimiter consumerRateLimiter = new ConsumerRateLimiter(subscription, outputRateCalculator, hermesMetrics,
                    consumerRateLimitSupervisor);

            return new SerialConsumer(
                    messageReceiver,
                    hermesMetrics,
                    subscription,
                    consumerRateLimiter,
                    subscriptionOffsetCommitQueues,
                    consumerMessageSenderFactory.create(subscription, consumerRateLimiter, subscriptionOffsetCommitQueues, inflightSemaphore),
                    inflightSemaphore,
                    trackers,
                    messageConverterResolver,
                    topic);
        }
    }
}
