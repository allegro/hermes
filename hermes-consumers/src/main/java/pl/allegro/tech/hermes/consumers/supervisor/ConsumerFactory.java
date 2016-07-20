package pl.allegro.tech.hermes.consumers.supervisor;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.BatchConsumer;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerMessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.SerialConsumer;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchFactory;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimitSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculator;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSenderFactory;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import javax.inject.Inject;

public class ConsumerFactory {

    private final ConsumerRateLimitSupervisor consumerRateLimitSupervisor;
    private final OutputRateCalculator outputRateCalculator;
    private final ReceiverFactory messageReceiverFactory;
    private final HermesMetrics hermesMetrics;
    private final ConfigFactory configFactory;
    private final Trackers trackers;
    private final ConsumerMessageSenderFactory consumerMessageSenderFactory;
    private final TopicRepository topicRepository;
    private final MessageConverterResolver messageConverterResolver;
    private final MessageBatchFactory batchFactory;
    private final MessageContentWrapper messageContentWrapper;
    private final MessageBatchSenderFactory batchSenderFactory;
    private final OffsetQueue offsetQueue;

    @Inject
    public ConsumerFactory(ReceiverFactory messageReceiverFactory,
                           HermesMetrics hermesMetrics,
                           ConfigFactory configFactory,
                           ConsumerRateLimitSupervisor consumerRateLimitSupervisor,
                           OutputRateCalculator outputRateCalculator,
                           Trackers trackers,
                           ConsumerMessageSenderFactory consumerMessageSenderFactory,
                           TopicRepository topicRepository,
                           MessageConverterResolver messageConverterResolver,
                           MessageBatchFactory byteBufferMessageBatchFactory,
                           MessageContentWrapper messageContentWrapper,
                           MessageBatchSenderFactory batchSenderFactory,
                           OffsetQueue offsetQueue) {

        this.messageReceiverFactory = messageReceiverFactory;
        this.hermesMetrics = hermesMetrics;
        this.configFactory = configFactory;
        this.consumerRateLimitSupervisor = consumerRateLimitSupervisor;
        this.outputRateCalculator = outputRateCalculator;
        this.trackers = trackers;
        this.consumerMessageSenderFactory = consumerMessageSenderFactory;
        this.topicRepository = topicRepository;
        this.messageConverterResolver = messageConverterResolver;
        this.batchFactory = byteBufferMessageBatchFactory;
        this.messageContentWrapper = messageContentWrapper;
        this.batchSenderFactory = batchSenderFactory;
        this.offsetQueue = offsetQueue;
    }

    Consumer createConsumer(Subscription subscription) {
        Topic topic = topicRepository.getTopicDetails(subscription.getTopicName());
        if (subscription.isBatchSubscription()) {
            return new BatchConsumer(messageReceiverFactory,
                    batchSenderFactory.create(subscription),
                    batchFactory,
                    offsetQueue,
                    messageConverterResolver,
                    messageContentWrapper,
                    hermesMetrics,
                    trackers,
                    subscription,
                    topic);
        } else {
            ConsumerRateLimiter consumerRateLimiter = new ConsumerRateLimiter(subscription, outputRateCalculator, hermesMetrics,
                    consumerRateLimitSupervisor);

            return new SerialConsumer(
                    messageReceiverFactory,
                    hermesMetrics,
                    subscription,
                    consumerRateLimiter,
                    consumerMessageSenderFactory,
                    trackers,
                    messageConverterResolver,
                    topic,
                    configFactory, offsetQueue);
        }
    }
}
