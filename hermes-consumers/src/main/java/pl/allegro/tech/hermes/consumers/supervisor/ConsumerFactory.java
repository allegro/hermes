package pl.allegro.tech.hermes.consumers.supervisor;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerMessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterFactory;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimitSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculator;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

import javax.inject.Inject;
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
    private final MessageConverterFactory messageConverterFactory;
    private final SchemaRepository<Schema> schemaRepository;

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
            MessageConverterFactory messageConverterFactory,
            SchemaRepository<Schema> schemaRepository) {

        this.messageReceiverFactory = messageReceiverFactory;
        this.hermesMetrics = hermesMetrics;
        this.configFactory = configFactory;
        this.consumerRateLimitSupervisor = consumerRateLimitSupervisor;
        this.outputRateCalculator = outputRateCalculator;
        this.trackers = trackers;
        this.consumerMessageSenderFactory = consumerMessageSenderFactory;
        this.clock = clock;
        this.topicRepository = topicRepository;
        this.messageConverterFactory = messageConverterFactory;
        this.schemaRepository = schemaRepository;
    }

    Consumer createConsumer(Subscription subscription) {
        SubscriptionOffsetCommitQueues subscriptionOffsetCommitQueues = new SubscriptionOffsetCommitQueues(
                subscription, hermesMetrics, clock, configFactory);

        ConsumerRateLimiter consumerRateLimiter = new ConsumerRateLimiter(subscription, outputRateCalculator, hermesMetrics,
                consumerRateLimitSupervisor);

        Semaphore inflightSemaphore = new Semaphore(configFactory.getIntProperty(CONSUMER_INFLIGHT_SIZE));

        Topic topic = topicRepository.getTopicDetails(subscription.getTopicName());

        return new Consumer(
            messageReceiverFactory.createMessageReceiver(topic.getContentType(), subscription),
            hermesMetrics,
            subscription,
            consumerRateLimiter,
            subscriptionOffsetCommitQueues,
            consumerMessageSenderFactory.create(subscription, consumerRateLimiter, subscriptionOffsetCommitQueues, inflightSemaphore),
            inflightSemaphore,
            trackers,
            messageConverterFactory.create(topic.getContentType(), schemaRepository),
            topic);
    }

}
