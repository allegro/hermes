package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilteredMessageHandler;
import pl.allegro.tech.hermes.consumers.consumer.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import javax.inject.Inject;
import java.time.Clock;
import java.util.Properties;

public class KafkaMessageReceiverFactory implements ReceiverFactory {

    private final ConfigFactory configFactory;
    private final MessageContentWrapper messageContentWrapper;
    private final HermesMetrics hermesMetrics;
    private final OffsetQueue offsetQueue;
    private final Clock clock;
    private final KafkaNamesMapper kafkaNamesMapper;
    private final SchemaRepository schemaRepository;
    private final FilterChainFactory filterChainFactory;
    private final Trackers trackers;

    @Inject
    public KafkaMessageReceiverFactory(ConfigFactory configFactory,
                                       MessageContentWrapper messageContentWrapper,
                                       HermesMetrics hermesMetrics,
                                       OffsetQueue offsetQueue,
                                       Clock clock,
                                       KafkaNamesMapper kafkaNamesMapper,
                                       SchemaRepository schemaRepository,
                                       FilterChainFactory filterChainFactory,
                                       Trackers trackers) {
        this.configFactory = configFactory;
        this.messageContentWrapper = messageContentWrapper;
        this.hermesMetrics = hermesMetrics;
        this.offsetQueue = offsetQueue;
        this.clock = clock;
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.schemaRepository = schemaRepository;
        this.filterChainFactory = filterChainFactory;
        this.trackers = trackers;
    }

    @Override
    public MessageReceiver createMessageReceiver(Topic topic,
                                                 Subscription subscription,
                                                 ConsumerRateLimiter consumerRateLimiter) {
        return create(topic,
                createConsumerConfig(kafkaNamesMapper.toConsumerGroupId(subscription.getQualifiedName())),
                subscription,
                consumerRateLimiter);
    }

    MessageReceiver create(Topic receivingTopic,
                           ConsumerConfig consumerConfig,
                           Subscription subscription,
                           ConsumerRateLimiter consumerRateLimiter) {
        MessageReceiver receiver = new KafkaMessageReceiver(
                receivingTopic,
                Consumer.createJavaConsumerConnector(consumerConfig),
                messageContentWrapper,
                hermesMetrics.timer(Timers.READ_LATENCY),
                clock,
                kafkaNamesMapper,
                configFactory.getIntProperty(Configs.KAFKA_STREAM_COUNT),
                configFactory.getIntProperty(Configs.KAFKA_CONSUMER_TIMEOUT_MS),
                subscription,
                schemaRepository);

        if (configFactory.getBooleanProperty(Configs.CONSUMER_FILTERING_ENABLED)) {
            FilteredMessageHandler filteredMessageHandler = new FilteredMessageHandler(
                    offsetQueue,
                    consumerRateLimiter,
                    trackers,
                    hermesMetrics);
            receiver = new FilteringMessageReceiver(receiver, filteredMessageHandler, filterChainFactory, subscription);
        }
        return receiver;
    }

    private ConsumerConfig createConsumerConfig(ConsumerGroupId groupId) {
        Properties props = new Properties();

        props.put("group.id", groupId.asString());
        props.put("zookeeper.connect", configFactory.getStringProperty(Configs.KAFKA_ZOOKEEPER_CONNECT_STRING));
        props.put("zookeeper.connection.timeout.ms", configFactory.getIntPropertyAsString(Configs.ZOOKEEPER_CONNECTION_TIMEOUT));
        props.put("zookeeper.session.timeout.ms", configFactory.getIntPropertyAsString(Configs.ZOOKEEPER_SESSION_TIMEOUT));
        props.put("auto.commit.enable", "false");
        props.put("fetch.wait.max.ms", "10000");
        props.put("consumer.timeout.ms", configFactory.getIntPropertyAsString(Configs.KAFKA_CONSUMER_TIMEOUT_MS));
        props.put("auto.offset.reset", configFactory.getStringProperty(Configs.KAFKA_CONSUMER_AUTO_OFFSET_RESET));
        props.put("offsets.storage", configFactory.getStringProperty(Configs.KAFKA_CONSUMER_OFFSETS_STORAGE));
        props.put("dual.commit.enabled", Boolean.toString(configFactory.getBooleanProperty(Configs.KAFKA_CONSUMER_DUAL_COMMIT_ENABLED)));
        props.put("rebalance.max.retries", configFactory.getIntPropertyAsString(Configs.KAFKA_CONSUMER_REBALANCE_MAX_RETRIES));
        props.put("rebalance.backoff.ms", configFactory.getIntPropertyAsString(Configs.KAFKA_CONSUMER_REBALANCE_BACKOFF));

        return new ConsumerConfig(props);
    }
}
