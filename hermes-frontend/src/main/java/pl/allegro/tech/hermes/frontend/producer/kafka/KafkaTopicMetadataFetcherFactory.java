package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;

import java.time.Duration;
import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;

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
        AdminClient adminClient = AdminClient.create(props);
        return new KafkaTopicMetadataFetcher(adminClient, metadataMaxAge);
    }
}
