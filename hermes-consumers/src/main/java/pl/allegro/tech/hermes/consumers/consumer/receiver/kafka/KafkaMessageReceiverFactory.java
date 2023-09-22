package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.CommonConsumerParameters;
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

import java.util.List;
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

public class KafkaMessageReceiverFactory implements ReceiverFactory {

    private final CommonConsumerParameters commonConsumerParameters;
    private final KafkaParameters kafkaAuthorizationParameters;
    private final KafkaReceiverParameters consumerReceiverParameters;
    private final KafkaConsumerParameters kafkaConsumerParameters;
    private final KafkaConsumerRecordToMessageConverterFactory messageConverterFactory;
    private final MetricsFacade metricsFacade;
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
                                       MetricsFacade metricsFacade,
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
        this.metricsFacade = metricsFacade;
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
                                                 MetricsFacade metrics) {

        MessageReceiver receiver = createKafkaSingleThreadedMessageReceiver(topic, subscription, loadReporter);

        if (consumerReceiverParameters.isWaitBetweenUnsuccessfulPolls()) {
            receiver = createThrottlingMessageReceiver(receiver, subscription, metrics);
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
                metricsFacade,
                kafkaNamesMapper,
                topic,
                subscription,
                consumerReceiverParameters.getPoolTimeout(),
                consumerReceiverParameters.getReadQueueCapacity(),
                loadReporter,
                consumerPartitionAssignmentState
        );
    }

    private MessageReceiver createThrottlingMessageReceiver(MessageReceiver receiver,
                                                            Subscription subscription,
                                                            MetricsFacade metrics) {
        IdleTimeCalculator idleTimeCalculator = new ExponentiallyGrowingIdleTimeCalculator(
                consumerReceiverParameters.getInitialIdleTime().toMillis(),
                consumerReceiverParameters.getMaxIdleTime().toMillis());

        return new ThrottlingMessageReceiver(receiver, idleTimeCalculator, subscription.getQualifiedName(), metrics);
    }

    private MessageReceiver createFilteringMessageReceiver(MessageReceiver receiver,
                                                           ConsumerRateLimiter consumerRateLimiter,
                                                           Subscription subscription,
                                                           MetricsFacade metrics) {
        boolean filteringRateLimitEnabled = consumerReceiverParameters.isFilteringRateLimiterEnabled();
        FilteredMessageHandler filteredMessageHandler = new FilteredMessageHandler(
                offsetQueue,
                filteringRateLimitEnabled ? consumerRateLimiter : null,
                trackers,
                metrics,
                subscription.getQualifiedName()
        );
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
            props.put(SASL_JAAS_CONFIG, kafkaAuthorizationParameters.getJaasConfig());
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
