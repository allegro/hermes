package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilteredMessageHandler;
import pl.allegro.tech.hermes.consumers.consumer.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import javax.inject.Inject;
import java.time.Clock;
import java.util.Properties;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

public class KafkaMessageReceiverFactory implements ReceiverFactory {

    private final ConfigFactory configs;
    private final MessageContentWrapper messageContentWrapper;
    private final HermesMetrics hermesMetrics;
    private final Clock clock;
    private final KafkaNamesMapper kafkaNamesMapper;
    private final SchemaRepository schemaRepository;
    private final FilterChainFactory filterChainFactory;
    private final Trackers trackers;

    @Inject
    public KafkaMessageReceiverFactory(ConfigFactory configs,
                                       MessageContentWrapper messageContentWrapper,
                                       HermesMetrics hermesMetrics,
                                       Clock clock,
                                       KafkaNamesMapper kafkaNamesMapper,
                                       SchemaRepository schemaRepository,
                                       FilterChainFactory filterChainFactory,
                                       Trackers trackers) {
        this.configs = configs;
        this.messageContentWrapper = messageContentWrapper;
        this.hermesMetrics = hermesMetrics;
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

        MessageReceiver receiver = new KafkaSingleThreadedMessageReceiver(
                createKafkaConsumer(kafkaNamesMapper.toConsumerGroupId(subscription.getQualifiedName())),
                messageContentWrapper,
                hermesMetrics,
                schemaRepository,
                kafkaNamesMapper,
                topic,
                subscription,
                clock,
                configs.getIntProperty(Configs.CONSUMER_RECEIVER_POOL_TIMEOUT),
                configs.getIntProperty(Configs.CONSUMER_RECEIVER_READ_QUEUE_CAPACITY));

        if (configs.getBooleanProperty(Configs.CONSUMER_FILTERING_ENABLED)) {
            FilteredMessageHandler filteredMessageHandler = new FilteredMessageHandler(
                    consumerRateLimiter,
                    trackers,
                    hermesMetrics);
            receiver = new FilteringMessageReceiver(receiver, filteredMessageHandler, filterChainFactory, subscription);
        }
        return receiver;
    }

    private KafkaConsumer<byte[], byte[]> createKafkaConsumer(ConsumerGroupId groupId) {
        Properties props = new Properties();
        props.put(GROUP_ID_CONFIG, groupId.asString());
        props.put(ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        props.put(CLIENT_ID_CONFIG, configs.getStringProperty(Configs.CONSUMER_CLIENT_ID) + "_" + groupId.asString());
        props.put(BOOTSTRAP_SERVERS_CONFIG, configs.getStringProperty(Configs.KAFKA_BROKER_LIST));
        props.put(AUTO_OFFSET_RESET_CONFIG, configs.getStringProperty(Configs.KAFKA_CONSUMER_AUTO_OFFSET_RESET_CONFIG));
        props.put(SESSION_TIMEOUT_MS_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_SESSION_TIMEOUT_MS_CONFIG));
        props.put(HEARTBEAT_INTERVAL_MS_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG));
        props.put(METADATA_MAX_AGE_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_METADATA_MAX_AGE_CONFIG));
        props.put(MAX_PARTITION_FETCH_BYTES_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_MAX_PARTITION_FETCH_BYTES_CONFIG));
        props.put(SEND_BUFFER_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_SEND_BUFFER_CONFIG));
        props.put(RECEIVE_BUFFER_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_RECEIVE_BUFFER_CONFIG));
        props.put(FETCH_MIN_BYTES_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_FETCH_MIN_BYTES_CONFIG));
        props.put(FETCH_MAX_WAIT_MS_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_FETCH_MAX_WAIT_MS_CONFIG));
        props.put(RECONNECT_BACKOFF_MS_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_RECONNECT_BACKOFF_MS_CONFIG));
        props.put(RETRY_BACKOFF_MS_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_RETRY_BACKOFF_MS_CONFIG));
        props.put(CHECK_CRCS_CONFIG, configs.getBooleanProperty(Configs.KAFKA_CONSUMER_CHECK_CRCS_CONFIG));
        props.put(METRICS_SAMPLE_WINDOW_MS_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_METRICS_SAMPLE_WINDOW_MS_CONFIG));
        props.put(METRICS_NUM_SAMPLES_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_METRICS_NUM_SAMPLES_CONFIG));
        props.put(REQUEST_TIMEOUT_MS_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_REQUEST_TIMEOUT_MS_CONFIG));
        props.put(CONNECTIONS_MAX_IDLE_MS_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_CONNECTIONS_MAX_IDLE_MS_CONFIG));
        props.put(MAX_POLL_RECORDS_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_MAX_POLL_RECORDS_CONFIG));
        return new KafkaConsumer<byte[], byte[]>(props);
    }
}
