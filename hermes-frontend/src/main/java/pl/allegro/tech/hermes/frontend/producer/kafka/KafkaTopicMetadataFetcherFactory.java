package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import pl.allegro.tech.hermes.common.config.KafkaSSLProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
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

public class KafkaTopicMetadataFetcherFactory {

    private final KafkaParameters kafkaParameters;

    private final Duration metadataMaxAge;

    private final int requestTimeoutMs;

    public KafkaTopicMetadataFetcherFactory(KafkaParameters kafkaParameters, Duration metadataMaxAge, int requestTimeoutMs) {
        this.kafkaParameters = kafkaParameters;
        this.metadataMaxAge = metadataMaxAge;
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public KafkaTopicMetadataFetcher provide() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaParameters.getBrokerList());
        props.put(SECURITY_PROTOCOL_CONFIG, DEFAULT_SECURITY_PROTOCOL);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
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

        AdminClient adminClient = AdminClient.create(props);
        return new KafkaTopicMetadataFetcher(adminClient, metadataMaxAge);
    }
}
