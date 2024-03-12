package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BATCH_SIZE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BUFFER_MEMORY_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.COMPRESSION_TYPE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.LINGER_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_BLOCK_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_REQUEST_SIZE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.METADATA_MAX_AGE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRY_BACKOFF_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.SEND_BUFFER_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;

public class KafkaMessageSendersFactory {

    private static final String ACK_ALL = "-1";
    private static final String ACK_LEADER = "1";

    private final TopicMetadataLoadingExecutor topicMetadataLoadingExecutor;
    private final MinInSyncReplicasLoader localMinInSyncReplicasLoader;
    private final KafkaParameters kafkaParameters;
    private final List<KafkaParameters> remoteKafkaParameters;
    private final long bufferedSizeBytes;

    public KafkaMessageSendersFactory(KafkaParameters kafkaParameters,
                                      List<KafkaParameters> remoteKafkaParameters,
                                      AdminClient localAdminClient,
                                      TopicsCache topicsCache,
                                      int retryCount,
                                      Duration retryInterval,
                                      int threadPoolSize,
                                      long bufferedSizeBytes,
                                      Duration metadataMaxAge) {
        this.topicMetadataLoadingExecutor = new TopicMetadataLoadingExecutor(topicsCache, retryCount, retryInterval, threadPoolSize);
        this.localMinInSyncReplicasLoader = new MinInSyncReplicasLoader(localAdminClient, metadataMaxAge);
        this.bufferedSizeBytes = bufferedSizeBytes;
        this.kafkaParameters = kafkaParameters;
        this.remoteKafkaParameters = remoteKafkaParameters;
    }

    public KafkaMessageSenders provide(KafkaProducerParameters kafkaProducerParameters) {
        KafkaMessageSenders.Tuple localProducers = new KafkaMessageSenders.Tuple(
                sender(kafkaParameters, kafkaProducerParameters, ACK_LEADER),
                sender(kafkaParameters, kafkaProducerParameters, ACK_ALL)
        );

        List<KafkaMessageSenders.Tuple> remoteProducers = remoteKafkaParameters.stream().map(
                kafkaProperties -> new KafkaMessageSenders.Tuple(
                        sender(kafkaProperties, kafkaProducerParameters, ACK_LEADER),
                        sender(kafkaProperties, kafkaProducerParameters, ACK_ALL))).toList();
        return new KafkaMessageSenders(
                topicMetadataLoadingExecutor,
                localMinInSyncReplicasLoader,
                localProducers,
                remoteProducers
        );
    }

    private KafkaMessageSender<byte[], byte[]> sender(KafkaParameters kafkaParameters,
                                                      KafkaProducerParameters kafkaProducerParameters,
                                                      String acks) {
        Map<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaParameters.getBrokerList());
        props.put(MAX_BLOCK_MS_CONFIG, (int) kafkaProducerParameters.getMaxBlock().toMillis());
        props.put(COMPRESSION_TYPE_CONFIG, kafkaProducerParameters.getCompressionCodec());
        props.put(BUFFER_MEMORY_CONFIG, bufferedSizeBytes);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, (int) kafkaProducerParameters.getRequestTimeout().toMillis());
        props.put(BATCH_SIZE_CONFIG, kafkaProducerParameters.getBatchSize());
        props.put(SEND_BUFFER_CONFIG, kafkaProducerParameters.getTcpSendBuffer());
        props.put(RETRIES_CONFIG, kafkaProducerParameters.getRetries());
        props.put(RETRY_BACKOFF_MS_CONFIG, (int) kafkaProducerParameters.getRetryBackoff().toMillis());
        props.put(METADATA_MAX_AGE_CONFIG, (int) kafkaProducerParameters.getMetadataMaxAge().toMillis());
        props.put(KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put(MAX_REQUEST_SIZE_CONFIG, kafkaProducerParameters.getMaxRequestSize());
        props.put(LINGER_MS_CONFIG, (int) kafkaProducerParameters.getLinger().toMillis());
        props.put(METRICS_SAMPLE_WINDOW_MS_CONFIG, (int) kafkaProducerParameters.getMetricsSampleWindow().toMillis());
        props.put(MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, kafkaProducerParameters.getMaxInflightRequestsPerConnection());
        props.put(ACKS_CONFIG, acks);

        if (kafkaParameters.isAuthenticationEnabled()) {
            props.put(SASL_MECHANISM, kafkaParameters.getAuthenticationMechanism());
            props.put(SECURITY_PROTOCOL_CONFIG, kafkaParameters.getAuthenticationProtocol());
            props.put(SASL_JAAS_CONFIG, kafkaParameters.getJaasConfig());
        }
        return new KafkaMessageSender<>(
                new org.apache.kafka.clients.producer.KafkaProducer<>(props),
                kafkaParameters.getDatacenter()
        );
    }

    public void close() throws Exception {
        topicMetadataLoadingExecutor.close();
        localMinInSyncReplicasLoader.close();
    }
}
