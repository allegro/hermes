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
import pl.allegro.tech.hermes.consumers.consumer.idleTime.ExponentiallyGrowingIdleTimeCalculator;
import pl.allegro.tech.hermes.consumers.consumer.idleTime.IdleTimeCalculator;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ThrottlingMessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import javax.inject.Inject;
import java.time.Clock;
import java.util.Properties;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_RECEIVER_INITIAL_IDLE_TIME;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_RECEIVER_MAX_IDLE_TIME;

public class KafkaMessageReceiverFactory implements ReceiverFactory {

    private final ConfigFactory configs;
    private final MessageContentWrapper messageContentWrapper;
    private final HermesMetrics hermesMetrics;
    private OffsetQueue offsetQueue;
    private final Clock clock;
    private final KafkaNamesMapper kafkaNamesMapper;
    private final FilterChainFactory filterChainFactory;
    private final Trackers trackers;

    @Inject
    public KafkaMessageReceiverFactory(ConfigFactory configs,
                                       MessageContentWrapper messageContentWrapper,
                                       HermesMetrics hermesMetrics,
                                       OffsetQueue offsetQueue,
                                       Clock clock,
                                       KafkaNamesMapper kafkaNamesMapper,
                                       FilterChainFactory filterChainFactory,
                                       Trackers trackers) {
        this.configs = configs;
        this.messageContentWrapper = messageContentWrapper;
        this.hermesMetrics = hermesMetrics;
        this.offsetQueue = offsetQueue;
        this.clock = clock;
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.filterChainFactory = filterChainFactory;
        this.trackers = trackers;
    }

    @Override
    public MessageReceiver createMessageReceiver(Topic topic,
                                                 Subscription subscription,
                                                 ConsumerRateLimiter consumerRateLimiter) {

        MessageReceiver receiver = new KafkaSingleThreadedMessageReceiver(
                createKafkaConsumer(topic, subscription),
                messageContentWrapper,
                hermesMetrics,
                kafkaNamesMapper,
                topic,
                subscription,
                clock,
                configs.getIntProperty(Configs.CONSUMER_RECEIVER_POOL_TIMEOUT),
                configs.getIntProperty(Configs.CONSUMER_RECEIVER_READ_QUEUE_CAPACITY));


        if (configs.getBooleanProperty(Configs.CONSUMER_RECEIVER_WAIT_BETWEEN_UNSUCCESSFUL_POLLS)) {
            IdleTimeCalculator idleTimeCalculator = new ExponentiallyGrowingIdleTimeCalculator(
                    configs.getIntProperty(CONSUMER_RECEIVER_INITIAL_IDLE_TIME),
                    configs.getIntProperty(CONSUMER_RECEIVER_MAX_IDLE_TIME)
            );
            receiver = new ThrottlingMessageReceiver(receiver, idleTimeCalculator, subscription, hermesMetrics);
        }

        if (configs.getBooleanProperty(Configs.CONSUMER_FILTERING_ENABLED)) {
            FilteredMessageHandler filteredMessageHandler = new FilteredMessageHandler(
                    offsetQueue,
                    consumerRateLimiter,
                    trackers,
                    hermesMetrics);
            receiver = new FilteringMessageReceiver(receiver, filteredMessageHandler, filterChainFactory, subscription);
        }
        return receiver;
    }

    private KafkaConsumer<byte[], byte[]> createKafkaConsumer(Topic topic, Subscription subscription) {
        ConsumerGroupId groupId = kafkaNamesMapper.toConsumerGroupId(subscription.getQualifiedName());
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
        props.put(MAX_PARTITION_FETCH_BYTES_CONFIG, getMaxPartitionFetch(topic, configs));
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
        props.put(MAX_POLL_INTERVAL_MS_CONFIG, configs.getIntProperty(Configs.KAFKA_CONSUMER_MAX_POLL_INTERVAL_CONFIG));
        return new KafkaConsumer<>(props);
    }

    private int getMaxPartitionFetch(Topic topic, ConfigFactory configs) {
        if (configs.getBooleanProperty(Configs.CONSUMER_USE_TOPIC_MESSAGE_SIZE)) {
            int topicMessageSize = topic.getMaxMessageSize();
            int min = configs.getIntProperty(Configs.KAFKA_CONSUMER_MAX_PARTITION_FETCH_MIN_BYTES_CONFIG);
            int max = configs.getIntProperty(Configs.KAFKA_CONSUMER_MAX_PARTITION_FETCH_MAX_BYTES_CONFIG);
            return Math.max(Math.min(topicMessageSize, max), min);
        } else {
            return configs.getIntProperty(Configs.KAFKA_CONSUMER_MAX_PARTITION_FETCH_MAX_BYTES_CONFIG);
        }
    }
}
