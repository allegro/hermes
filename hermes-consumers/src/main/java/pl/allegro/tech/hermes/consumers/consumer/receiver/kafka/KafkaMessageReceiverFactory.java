package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.KafkaSSLProperties;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.CommonConsumerParameters;
import pl.allegro.tech.hermes.consumers.consumer.SubscriptionMetrics;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilteredMessageHandler;
import pl.allegro.tech.hermes.consumers.consumer.idletime.ExponentiallyGrowingIdleTimeCalculator;
import pl.allegro.tech.hermes.consumers.consumer.idletime.IdleTimeCalculator;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ThrottlingMessageReceiver;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static java.util.stream.Collectors.toList;
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
import static org.apache.kafka.clients.consumer.ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.RECEIVE_BUFFER_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.RETRY_BACKOFF_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.SEND_BUFFER_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static org.apache.kafka.common.config.SslConfigs.SSL_CIPHER_SUITES_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_ENGINE_FACTORY_CLASS_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYMANAGER_ALGORITHM_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_KEY_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_TYPE_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEY_PASSWORD_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_PROVIDER_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_SECURE_RANDOM_IMPLEMENTATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTMANAGER_ALGORITHM_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG;

public class KafkaMessageReceiverFactory implements ReceiverFactory {

    private final CommonConsumerParameters commonConsumerParameters;
    private final KafkaParameters kafkaAuthorizationParameters;
    private final KafkaReceiverParameters consumerReceiverParameters;
    private final KafkaConsumerParameters kafkaConsumerParameters;
    private final KafkaConsumerRecordToMessageConverterFactory messageConverterFactory;
    private final HermesMetrics hermesMetrics;
    private final OffsetQueue offsetQueue;
    private final KafkaNamesMapper kafkaNamesMapper;
    private final FilterChainFactory filterChainFactory;
    private final Trackers trackers;
    private final ConsumerPartitionAssignmentState consumerPartitionAssignmentState;

    public KafkaMessageReceiverFactory(CommonConsumerParameters commonConsumerParameters,
                                       KafkaReceiverParameters consumerReceiverParameters,
                                       KafkaConsumerParameters kafkaConsumerParameters,
                                       KafkaParameters kafkaAuthorizationParameters,
                                       KafkaConsumerRecordToMessageConverterFactory messageConverterFactory,
                                       HermesMetrics hermesMetrics,
                                       OffsetQueue offsetQueue,
                                       KafkaNamesMapper kafkaNamesMapper,
                                       FilterChainFactory filterChainFactory,
                                       Trackers trackers,
                                       ConsumerPartitionAssignmentState consumerPartitionAssignmentState) {
        this.commonConsumerParameters = commonConsumerParameters;
        this.consumerReceiverParameters = consumerReceiverParameters;
        this.kafkaConsumerParameters = kafkaConsumerParameters;
        this.kafkaAuthorizationParameters = kafkaAuthorizationParameters;
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
                                                 ConsumerRateLimiter consumerRateLimiter,
                                                 SubscriptionLoadRecorder loadReporter,
                                                 SubscriptionMetrics metrics) {

        MessageReceiver receiver = createKafkaSingleThreadedMessageReceiver(topic, subscription, loadReporter);

        if (consumerReceiverParameters.isWaitBetweenUnsuccessfulPolls()) {
            receiver = createThrottlingMessageReceiver(receiver, metrics);
        }

        if (consumerReceiverParameters.isFilteringEnabled()) {
            receiver = createFilteringMessageReceiver(receiver, consumerRateLimiter, subscription, metrics);
        }

        return receiver;
    }

    private MessageReceiver createKafkaSingleThreadedMessageReceiver(Topic topic,
                                                                     Subscription subscription,
                                                                     SubscriptionLoadRecorder loadReporter) {
        return new KafkaSingleThreadedMessageReceiver(
                createKafkaConsumer(topic, subscription),
                messageConverterFactory,
                hermesMetrics,
                kafkaNamesMapper,
                topic,
                subscription,
                consumerReceiverParameters.getPoolTimeout(),
                consumerReceiverParameters.getReadQueueCapacity(),
                loadReporter,
                consumerPartitionAssignmentState
        );
    }

    private MessageReceiver createThrottlingMessageReceiver(MessageReceiver receiver, SubscriptionMetrics metrics) {
        IdleTimeCalculator idleTimeCalculator = new ExponentiallyGrowingIdleTimeCalculator(
                consumerReceiverParameters.getInitialIdleTime().toMillis(),
                consumerReceiverParameters.getMaxIdleTime().toMillis());

        return new ThrottlingMessageReceiver(receiver, idleTimeCalculator, metrics);
    }

    private MessageReceiver createFilteringMessageReceiver(MessageReceiver receiver,
                                                           ConsumerRateLimiter consumerRateLimiter,
                                                           Subscription subscription,
                                                           SubscriptionMetrics metrics) {
        boolean filteringRateLimitEnabled = consumerReceiverParameters.isFilteringRateLimiterEnabled();
        FilteredMessageHandler filteredMessageHandler = new FilteredMessageHandler(
                offsetQueue,
                filteringRateLimitEnabled ? consumerRateLimiter : null,
                trackers,
                metrics);
        return new FilteringMessageReceiver(receiver, filteredMessageHandler, filterChainFactory, subscription);
    }

    private KafkaConsumer<byte[], byte[]> createKafkaConsumer(Topic topic, Subscription subscription) {
        ConsumerGroupId groupId = kafkaNamesMapper.toConsumerGroupId(subscription.getQualifiedName());
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaAuthorizationParameters.getBrokerList());
        props.put(CLIENT_ID_CONFIG, consumerReceiverParameters.getClientId() + "_" + groupId.asString());
        props.put(GROUP_ID_CONFIG, groupId.asString());
        props.put(ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        addKafkaAuthorizationParameters(props);
        addKafkaConsumerParameters(props, topic);
        return new KafkaConsumer<>(props);
    }

    private void addKafkaAuthorizationParameters(Properties props) {
        if (kafkaAuthorizationParameters.isEnabled()) {
            props.put(SASL_MECHANISM, kafkaAuthorizationParameters.getMechanism());
            props.put(SECURITY_PROTOCOL_CONFIG, kafkaAuthorizationParameters.getProtocol());
            props.put(SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required\n"
                            + "username=\"" + kafkaAuthorizationParameters.getUsername() + "\"\n"
                            + "password=\"" + kafkaAuthorizationParameters.getPassword() + "\";"
            );
        }

        KafkaSSLProperties ssl = kafkaAuthorizationParameters.getSsl();
        if (ssl.isEnabled()) {
            Optional.ofNullable(ssl.getKeyPassword()).ifPresent(v -> props.put(SSL_KEY_PASSWORD_CONFIG, v));
            Optional.ofNullable(ssl.getKeyStoreCertificateChain()).ifPresent(v -> props.put(SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, v));
            Optional.ofNullable(ssl.getKeyStoreKey()).ifPresent(v -> props.put(SSL_KEYSTORE_KEY_CONFIG, v));
            Optional.ofNullable(ssl.getKeyStoreLocation()).ifPresent(v -> props.put(SSL_KEYSTORE_LOCATION_CONFIG, v));
            Optional.ofNullable(ssl.getKeyStorePassword()).ifPresent(v -> props.put(SSL_KEYSTORE_PASSWORD_CONFIG, v));
            Optional.ofNullable(ssl.getTrustStoreCertificates()).ifPresent(v -> props.put(SSL_TRUSTSTORE_CERTIFICATES_CONFIG, v));
            Optional.ofNullable(ssl.getTrustStoreLocation()).ifPresent(v -> props.put(SSL_TRUSTSTORE_LOCATION_CONFIG, v));
            Optional.ofNullable(ssl.getTrustStorePassword()).ifPresent(v -> props.put(SSL_TRUSTSTORE_PASSWORD_CONFIG, v));
            Optional.ofNullable(ssl.getEnabledProtocols()).map(s -> Arrays.asList(s.split(",")))
                    .ifPresent(v -> props.put(SSL_ENABLED_PROTOCOLS_CONFIG, v));
            Optional.ofNullable(ssl.getKeyStoreType()).ifPresent(v -> props.put(SSL_KEYSTORE_TYPE_CONFIG, v));
            Optional.ofNullable(ssl.getProtocol()).ifPresent(v -> props.put(SSL_PROTOCOL_CONFIG, v));
            Optional.ofNullable(ssl.getProvider()).ifPresent(v -> props.put(SSL_PROVIDER_CONFIG, v));
            Optional.ofNullable(ssl.getTrustStoreType()).ifPresent(v -> props.put(SSL_TRUSTSTORE_TYPE_CONFIG, v));
            Optional.ofNullable(ssl.getCipherSuites()).map(s -> Arrays.asList(s.split(",")))
                    .ifPresent(v -> props.put(SSL_CIPHER_SUITES_CONFIG, v));
            Optional.ofNullable(ssl.getEndpointIdentificationAlgorithm())
                    .ifPresent(v -> props.put(SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, v));
            Optional.ofNullable(ssl.getEngineFactoryClass()).ifPresent(v -> props.put(SSL_ENGINE_FACTORY_CLASS_CONFIG, v));
            Optional.ofNullable(ssl.getKeymanagerAlgorithm()).ifPresent(v -> props.put(SSL_KEYMANAGER_ALGORITHM_CONFIG, v));
            Optional.ofNullable(ssl.getSecureRandomImplementation()).ifPresent(v -> props.put(SSL_SECURE_RANDOM_IMPLEMENTATION_CONFIG, v));
            Optional.ofNullable(ssl.getTrustmanagerAlgorithm()).ifPresent(v -> props.put(SSL_TRUSTMANAGER_ALGORITHM_CONFIG, v));
        }
    }

    private void addKafkaConsumerParameters(Properties props, Topic topic) {
        props.put(AUTO_OFFSET_RESET_CONFIG, kafkaConsumerParameters.getAutoOffsetReset());
        props.put(SESSION_TIMEOUT_MS_CONFIG, (int) kafkaConsumerParameters.getSessionTimeout().toMillis());
        props.put(HEARTBEAT_INTERVAL_MS_CONFIG, (int) kafkaConsumerParameters.getHeartbeatInterval().toMillis());
        props.put(METADATA_MAX_AGE_CONFIG, (int) kafkaConsumerParameters.getMetadataMaxAge().toMillis());
        props.put(MAX_PARTITION_FETCH_BYTES_CONFIG, getMaxPartitionFetch(topic));
        props.put(SEND_BUFFER_CONFIG, kafkaConsumerParameters.getSendBufferBytes());
        props.put(RECEIVE_BUFFER_CONFIG, kafkaConsumerParameters.getReceiveBufferBytes());
        props.put(FETCH_MIN_BYTES_CONFIG, kafkaConsumerParameters.getFetchMinBytes());
        props.put(FETCH_MAX_WAIT_MS_CONFIG, (int) kafkaConsumerParameters.getFetchMaxWait().toMillis());
        props.put(RECONNECT_BACKOFF_MS_CONFIG, (int) kafkaConsumerParameters.getReconnectBackoff().toMillis());
        props.put(RETRY_BACKOFF_MS_CONFIG, (int) kafkaConsumerParameters.getRetryBackoff().toMillis());
        props.put(CHECK_CRCS_CONFIG, kafkaConsumerParameters.isCheckCrcs());
        props.put(METRICS_SAMPLE_WINDOW_MS_CONFIG, (int) kafkaConsumerParameters.getMetricsSampleWindow().toMillis());
        props.put(METRICS_NUM_SAMPLES_CONFIG, kafkaConsumerParameters.getMetricsNumSamples());
        props.put(REQUEST_TIMEOUT_MS_CONFIG, (int) kafkaConsumerParameters.getRequestTimeout().toMillis());
        props.put(CONNECTIONS_MAX_IDLE_MS_CONFIG, (int) kafkaConsumerParameters.getConnectionsMaxIdle().toMillis());
        props.put(MAX_POLL_RECORDS_CONFIG, kafkaConsumerParameters.getMaxPollRecords());
        props.put(MAX_POLL_INTERVAL_MS_CONFIG, (int) kafkaConsumerParameters.getMaxPollInterval().toMillis());
        props.put(PARTITION_ASSIGNMENT_STRATEGY_CONFIG, getPartitionAssignmentStrategies());
    }

    private int getMaxPartitionFetch(Topic topic) {
        if (commonConsumerParameters.isUseTopicMessageSizeEnabled()) {
            int topicMessageSize = topic.getMaxMessageSize();
            int min = kafkaConsumerParameters.getMaxPartitionFetchMin();
            int max = kafkaConsumerParameters.getMaxPartitionFetchMax();
            return Math.max(Math.min(topicMessageSize, max), min);
        } else {
            return kafkaConsumerParameters.getMaxPartitionFetchMax();
        }
    }

    private List<String> getPartitionAssignmentStrategies() {
        return kafkaConsumerParameters.getPartitionAssignmentStrategies().stream()
                .map(PartitionAssignmentStrategy::getAssignorClass)
                .map(Class::getName)
                .collect(toList());
    }
}
