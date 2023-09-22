package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;

import java.util.HashMap;
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

public class KafkaMessageProducerFactory {

    private static final String ACK_ALL = "-1";
    private static final String ACK_LEADER = "1";

    private final KafkaParameters kafkaParameters;
    private final KafkaProducerParameters kafkaProducerParameters;
    private final long bufferedSizeBytes;

    public KafkaMessageProducerFactory(KafkaParameters kafkaParameters,
                                       KafkaProducerParameters kafkaProducerParameters,
                                       long bufferedSizeBytes) {
        this.kafkaProducerParameters = kafkaProducerParameters;
        this.bufferedSizeBytes = bufferedSizeBytes;
        this.kafkaParameters = kafkaParameters;
    }

    public Producers provide() {
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

        if (kafkaParameters.isEnabled()) {
            props.put(SASL_MECHANISM, kafkaParameters.getMechanism());
            props.put(SECURITY_PROTOCOL_CONFIG, kafkaParameters.getProtocol());
            props.put(SASL_JAAS_CONFIG, kafkaParameters.getJaasConfig());
        }

        Producer<byte[], byte[]> leaderConfirms = new KafkaProducer<>(copyWithEntryAdded(props, ACKS_CONFIG, ACK_LEADER));
        Producer<byte[], byte[]> everyoneConfirms = new KafkaProducer<>(copyWithEntryAdded(props, ACKS_CONFIG, ACK_ALL));
        return new Producers(leaderConfirms, everyoneConfirms, kafkaProducerParameters.isReportNodeMetricsEnabled());
    }

    private ImmutableMap<String, Object> copyWithEntryAdded(Map<String, Object> common, String key, String value) {
        return ImmutableMap.<String, Object>builder().putAll(common).put(key, value).build();
    }
}
