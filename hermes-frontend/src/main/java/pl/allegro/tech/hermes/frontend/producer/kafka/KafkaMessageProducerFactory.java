package pl.allegro.tech.hermes.frontend.producer.kafka;

import pl.allegro.tech.hermes.common.kafka.KafkaParameters;
import pl.allegro.tech.hermes.frontend.config.KafkaProperties;
import pl.allegro.tech.hermes.frontend.producer.BrokerLatencyReporter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BATCH_SIZE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BUFFER_MEMORY_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.COMPRESSION_TYPE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG;
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
    private final List<KafkaProperties> remoteKafkaParameters;
    private final KafkaProducerParameters kafkaProducerParameters;
    private final BrokerLatencyReporter brokerLatencyReporter;
    private final long bufferedSizeBytes;

    public KafkaMessageProducerFactory(KafkaParameters kafkaParameters,
                                       List<KafkaProperties> remoteKafkaParameters,
                                       KafkaProducerParameters kafkaProducerParameters, BrokerLatencyReporter brokerLatencyReporter,
                                       long bufferedSizeBytes) {
        this.kafkaProducerParameters = kafkaProducerParameters;
        this.brokerLatencyReporter = brokerLatencyReporter;
        this.bufferedSizeBytes = bufferedSizeBytes;
        this.kafkaParameters = kafkaParameters;
        this.remoteKafkaParameters = remoteKafkaParameters;

    }

    public Producers provide() {
        Producers.Tuple localProducers = new Producers.Tuple(
                producer(kafkaParameters, kafkaProducerParameters, ACK_LEADER),
                producer(kafkaParameters, kafkaProducerParameters, ACK_ALL)
        );

        List<Producers.Tuple> remoteProducers = remoteKafkaParameters.stream().map(
                kafkaProperties -> new Producers.Tuple(
                        producer(kafkaProperties, kafkaProducerParameters, ACK_LEADER),
                        producer(kafkaProperties, kafkaProducerParameters, ACK_ALL))).toList();
        return new Producers(
                localProducers,
                remoteProducers
        );
    }

    private KafkaProducer<byte[], byte[]> producer(KafkaParameters kafkaParameters,
                                                   KafkaProducerParameters kafkaProducerParameters,
                                                   String acks) {
        Map<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaParameters.getBrokerList());
        props.put(MAX_BLOCK_MS_CONFIG, (int) kafkaProducerParameters.getMaxBlock().toMillis());
        props.put(COMPRESSION_TYPE_CONFIG, kafkaProducerParameters.getCompressionCodec());
        props.put(BUFFER_MEMORY_CONFIG, bufferedSizeBytes);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, (int) kafkaProducerParameters.getRequestTimeout().toMillis());
        props.put(DELIVERY_TIMEOUT_MS_CONFIG, (int) kafkaProducerParameters.getDeliveryTimeout().toMillis());
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
        return new KafkaProducer<>(
                new org.apache.kafka.clients.producer.KafkaProducer<>(props),
                brokerLatencyReporter,
                kafkaParameters.getDatacenter()
        );
    }
}
