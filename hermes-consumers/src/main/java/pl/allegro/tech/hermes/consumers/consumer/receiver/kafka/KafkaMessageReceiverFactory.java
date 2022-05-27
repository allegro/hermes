package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilteredMessageHandler;
import pl.allegro.tech.hermes.consumers.consumer.idleTime.ExponentiallyGrowingIdleTimeCalculator;
import pl.allegro.tech.hermes.consumers.consumer.idleTime.IdleTimeCalculator;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ThrottlingMessageReceiver;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.CHECK_CRCS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MIN_BYTES_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.METADATA_MAX_AGE_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.METRICS_NUM_SAMPLES_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.RECEIVE_BUFFER_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.RETRY_BACKOFF_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.SEND_BUFFER_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_MECHANISM;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_PASSWORD;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_PROTOCOL;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_USERNAME;

public class KafkaMessageReceiverFactory implements ReceiverFactory {

    private final ConfigFactory configs;
    private final KafkaReceiverParameters consumerReceiverParameters;
    private final KafkaConsumerParameters kafkaConsumerParameters;
    private final KafkaConsumerRecordToMessageConverterFactory messageConverterFactory;
    private final HermesMetrics hermesMetrics;
    private final OffsetQueue offsetQueue;
    private final KafkaNamesMapper kafkaNamesMapper;
    private final FilterChainFactory filterChainFactory;
    private final Trackers trackers;
    private final ConsumerPartitionAssignmentState consumerPartitionAssignmentState;

    public KafkaMessageReceiverFactory(ConfigFactory configs,
                                       KafkaReceiverParameters consumerReceiverParameters,
                                       KafkaConsumerParameters kafkaConsumerParameters,
                                       KafkaConsumerRecordToMessageConverterFactory messageConverterFactory,
                                       HermesMetrics hermesMetrics,
                                       OffsetQueue offsetQueue,
                                       KafkaNamesMapper kafkaNamesMapper,
                                       FilterChainFactory filterChainFactory,
                                       Trackers trackers,
                                       ConsumerPartitionAssignmentState consumerPartitionAssignmentState) {
        this.configs = configs;
        this.consumerReceiverParameters = consumerReceiverParameters;
        this.kafkaConsumerParameters = kafkaConsumerParameters;
        this.messageConverterFactory = messageConverterFactory;
        this.hermesMetrics = hermesMetrics;
        this.offsetQueue = offsetQueue;
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.filterChainFactory = filterChainFactory;
        this.trackers = trackers;
        this.consumerPartitionAssignmentState = consumerPartitionAssignmentState;
    }

    @Override
    public MessageReceiver createMessageReceiver(Topic topic,
                                                 Subscription subscription,
                                                 ConsumerRateLimiter consumerRateLimiter) {

        MessageReceiver receiver = createKafkaSingleThreadedMessageReceiver(topic, subscription);

        if (consumerReceiverParameters.isWaitBetweenUnsuccessfulPollsEnabled()) {
            receiver = createThrottlingMessageReceiver(receiver, subscription);
        }

        if (configs.getBooleanProperty(Configs.CONSUMER_FILTERING_ENABLED)) {
            receiver = createFilteringMessageReceiver(receiver, consumerRateLimiter, subscription);
        }

        return receiver;
    }

    private MessageReceiver createKafkaSingleThreadedMessageReceiver(Topic topic,
                                                                     Subscription subscription) {
        return new KafkaSingleThreadedMessageReceiver(
                createKafkaConsumer(topic, subscription),
                messageConverterFactory,
                hermesMetrics,
                kafkaNamesMapper,
                topic,
                subscription,
                consumerReceiverParameters.getPoolTimeout(),
                consumerReceiverParameters.getReadQueueCapacity(),
                consumerPartitionAssignmentState);
    }

    private MessageReceiver createThrottlingMessageReceiver(MessageReceiver receiver, Subscription subscription) {
        IdleTimeCalculator idleTimeCalculator = new ExponentiallyGrowingIdleTimeCalculator(
                consumerReceiverParameters.getInitialIdleTime(),
                consumerReceiverParameters.getMaxIdleTime());

        return new ThrottlingMessageReceiver(receiver, idleTimeCalculator, subscription, hermesMetrics);
    }

    private MessageReceiver createFilteringMessageReceiver(MessageReceiver receiver,
                                                           ConsumerRateLimiter consumerRateLimiter,
                                                           Subscription subscription) {
        boolean filteringRateLimitEnabled = configs.getBooleanProperty(Configs.CONSUMER_FILTERING_RATE_LIMITER_ENABLED);
        FilteredMessageHandler filteredMessageHandler = new FilteredMessageHandler(
                offsetQueue,
                filteringRateLimitEnabled ? consumerRateLimiter : null,
                trackers,
                hermesMetrics);
        return new FilteringMessageReceiver(receiver, filteredMessageHandler, filterChainFactory, subscription);
    }

    private KafkaConsumer<byte[], byte[]> createKafkaConsumer(Topic topic, Subscription subscription) {
        ConsumerGroupId groupId = kafkaNamesMapper.toConsumerGroupId(subscription.getQualifiedName());
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, configs.getStringProperty(Configs.KAFKA_BROKER_LIST));
        props.put(CLIENT_ID_CONFIG, configs.getStringProperty(Configs.CONSUMER_CLIENT_ID) + "_" + groupId.asString());
        props.put(GROUP_ID_CONFIG, groupId.asString());
        props.put(ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        addKafkaAuthorizationParameters(props);
        addKafkaConsumerParameters(props, topic);
        return new KafkaConsumer<>(props);
    }

    private void addKafkaAuthorizationParameters(Properties props) {
        if (configs.getBooleanProperty(KAFKA_AUTHORIZATION_ENABLED)) {
            props.put(SASL_MECHANISM, configs.getStringProperty(KAFKA_AUTHORIZATION_MECHANISM));
            props.put(SECURITY_PROTOCOL_CONFIG, configs.getStringProperty(KAFKA_AUTHORIZATION_PROTOCOL));
            props.put(SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required\n"
                            + "username=\"" + configs.getStringProperty(KAFKA_AUTHORIZATION_USERNAME) + "\"\n"
                            + "password=\"" + configs.getStringProperty(KAFKA_AUTHORIZATION_PASSWORD) + "\";"
            );
        }
    }

    private void addKafkaConsumerParameters(Properties props, Topic topic) {
        props.put(AUTO_OFFSET_RESET_CONFIG, kafkaConsumerParameters.getAutoOffsetReset());
        props.put(SESSION_TIMEOUT_MS_CONFIG, kafkaConsumerParameters.getSessionTimeoutMs());
        props.put(HEARTBEAT_INTERVAL_MS_CONFIG, kafkaConsumerParameters.getHeartbeatIntervalMs());
        props.put(METADATA_MAX_AGE_CONFIG, kafkaConsumerParameters.getMetadataMaxAgeMs());
        props.put(MAX_PARTITION_FETCH_BYTES_CONFIG, getMaxPartitionFetch(topic, configs));
        props.put(SEND_BUFFER_CONFIG, kafkaConsumerParameters.getSendBufferBytes());
        props.put(RECEIVE_BUFFER_CONFIG, kafkaConsumerParameters.getReceiveBufferBytes());
        props.put(FETCH_MIN_BYTES_CONFIG, kafkaConsumerParameters.getFetchMinBytes());
        props.put(FETCH_MAX_WAIT_MS_CONFIG, kafkaConsumerParameters.getFetchMaxWaitMs());
        props.put(RECONNECT_BACKOFF_MS_CONFIG, kafkaConsumerParameters.getReconnectBackoffMs());
        props.put(RETRY_BACKOFF_MS_CONFIG, kafkaConsumerParameters.getRetryBackoffMs());
        props.put(CHECK_CRCS_CONFIG, kafkaConsumerParameters.isCheckCrcsEnabled());
        props.put(METRICS_SAMPLE_WINDOW_MS_CONFIG, kafkaConsumerParameters.getMetricsSampleWindowMs());
        props.put(METRICS_NUM_SAMPLES_CONFIG, kafkaConsumerParameters.getMetricsNumSamples());
        props.put(REQUEST_TIMEOUT_MS_CONFIG, kafkaConsumerParameters.getRequestTimeoutMs());
        props.put(CONNECTIONS_MAX_IDLE_MS_CONFIG, kafkaConsumerParameters.getConnectionsMaxIdleMs());
        props.put(MAX_POLL_RECORDS_CONFIG, kafkaConsumerParameters.getMaxPollRecords());
        props.put(MAX_POLL_INTERVAL_MS_CONFIG, kafkaConsumerParameters.getMaxPollIntervalMs());
    }

    private int getMaxPartitionFetch(Topic topic, ConfigFactory configs) {
        if (configs.getBooleanProperty(Configs.CONSUMER_USE_TOPIC_MESSAGE_SIZE)) {
            int topicMessageSize = topic.getMaxMessageSize();
            int min = kafkaConsumerParameters.getMaxPartitionFetchMin();
            int max = kafkaConsumerParameters.getMaxPartitionFetchMax();
            return Math.max(Math.min(topicMessageSize, max), min);
        } else {
            return kafkaConsumerParameters.getMaxPartitionFetchMax();
        }
    }
}
