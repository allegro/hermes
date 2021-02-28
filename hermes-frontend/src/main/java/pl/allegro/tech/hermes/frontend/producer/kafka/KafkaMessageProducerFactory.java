package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import pl.allegro.tech.hermes.common.config.KafkaSSLProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
            props.put(SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required\n"
                            + "username=\"" + kafkaParameters.getUsername() + "\"\n"
                            + "password=\"" + kafkaParameters.getPassword() + "\";"
            );
        }

        KafkaSSLProperties ssl = kafkaParameters.getSsl();
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

        Producer<byte[], byte[]> leaderConfirms = new KafkaProducer<>(copyWithEntryAdded(props, ACKS_CONFIG, ACK_LEADER));
        Producer<byte[], byte[]> everyoneConfirms = new KafkaProducer<>(copyWithEntryAdded(props, ACKS_CONFIG, ACK_ALL));
        return new Producers(leaderConfirms, everyoneConfirms, kafkaProducerParameters.isReportNodeMetricsEnabled());
    }

    private ImmutableMap<String, Object> copyWithEntryAdded(Map<String, Object> common, String key, String value) {
        return ImmutableMap.<String, Object>builder().putAll(common).put(key, value).build();
    }
}
