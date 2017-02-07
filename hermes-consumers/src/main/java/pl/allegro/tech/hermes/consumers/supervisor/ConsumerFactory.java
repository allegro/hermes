package pl.allegro.tech.hermes.consumers.supervisor;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.BatchConsumer;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerAuthorizationHandler;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerMessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.SerialConsumer;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchFactory;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimitSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculatorFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSenderFactory;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import javax.inject.Inject;
import java.time.Clock;

public class ConsumerFactory {

    private final ConsumerRateLimitSupervisor consumerRateLimitSupervisor;
    private final OutputRateCalculatorFactory outputRateCalculatorFactory;
    private final ReceiverFactory messageReceiverFactory;
    private final HermesMetrics hermesMetrics;
    private final ConfigFactory configFactory;
    private final Trackers trackers;
    private final OffsetQueue offsetQueue;
    private final ConsumerMessageSenderFactory consumerMessageSenderFactory;
    private final TopicRepository topicRepository;
    private final MessageConverterResolver messageConverterResolver;
    private final MessageBatchFactory batchFactory;
    private final MessageContentWrapper messageContentWrapper;
    private final MessageBatchSenderFactory batchSenderFactory;
    private final ConsumerAuthorizationHandler consumerAuthorizationHandler;
    private final Clock clock;

    @Inject
    public ConsumerFactory(ReceiverFactory messageReceiverFactory,
                           HermesMetrics hermesMetrics,
                           ConfigFactory configFactory,
                           ConsumerRateLimitSupervisor consumerRateLimitSupervisor,
                           OutputRateCalculatorFactory outputRateCalculatorFactory,
                           Trackers trackers,
                           OffsetQueue offsetQueue,
                           ConsumerMessageSenderFactory consumerMessageSenderFactory,
                           TopicRepository topicRepository,
                           MessageConverterResolver messageConverterResolver,
                           MessageBatchFactory byteBufferMessageBatchFactory,
                           MessageContentWrapper messageContentWrapper,
                           MessageBatchSenderFactory batchSenderFactory,
                           ConsumerAuthorizationHandler consumerAuthorizationHandler,
                           Clock clock) {

        this.messageReceiverFactory = messageReceiverFactory;
        this.hermesMetrics = hermesMetrics;
        this.configFactory = configFactory;
        this.consumerRateLimitSupervisor = consumerRateLimitSupervisor;
        this.outputRateCalculatorFactory = outputRateCalculatorFactory;
        this.trackers = trackers;
        this.offsetQueue = offsetQueue;
        this.consumerMessageSenderFactory = consumerMessageSenderFactory;
        this.topicRepository = topicRepository;
        this.messageConverterResolver = messageConverterResolver;
        this.batchFactory = byteBufferMessageBatchFactory;
        this.messageContentWrapper = messageContentWrapper;
        this.batchSenderFactory = batchSenderFactory;
        this.consumerAuthorizationHandler = consumerAuthorizationHandler;
        this.clock = clock;
    }

    public Consumer createConsumer(Subscription subscription) {
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
                    topic,
                    configFactory);
        } else {
            SerialConsumerRateLimiter consumerRateLimiter = new SerialConsumerRateLimiter(subscription,
                    outputRateCalculatorFactory, hermesMetrics, consumerRateLimitSupervisor, clock);

            return new SerialConsumer(
                    messageReceiverFactory,
                    hermesMetrics,
                    subscription,
                    consumerRateLimiter,
                    consumerMessageSenderFactory,
                    trackers,
                    messageConverterResolver,
                    topic,
                    configFactory,
                    offsetQueue,
                    consumerAuthorizationHandler);
        }
    }
}
