package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import pl.allegro.tech.hermes.common.config.ConfigFactory;

import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_ADMIN_REQUEST_TIMEOUT_MS;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_MECHANISM;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_PASSWORD;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_PROTOCOL;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_USERNAME;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_BROKER_LIST;

public class KafkaTopicMetadataFetcherFactory {
    private final ConfigFactory configFactory;

    public KafkaTopicMetadataFetcherFactory(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    public KafkaTopicMetadataFetcher provide() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, configFactory.getStringProperty(KAFKA_BROKER_LIST));
        props.put(SECURITY_PROTOCOL_CONFIG, DEFAULT_SECURITY_PROTOCOL);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, configFactory.getIntProperty(KAFKA_ADMIN_REQUEST_TIMEOUT_MS));
        if (configFactory.getBooleanProperty(KAFKA_AUTHORIZATION_ENABLED)) {
            props.put(SASL_MECHANISM, configFactory.getStringProperty(KAFKA_AUTHORIZATION_MECHANISM));
            props.put(SECURITY_PROTOCOL_CONFIG, configFactory.getStringProperty(KAFKA_AUTHORIZATION_PROTOCOL));
            props.put(SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required\n"
                            + "username=\"" + configFactory.getStringProperty(KAFKA_AUTHORIZATION_USERNAME) + "\"\n"
                            + "password=\"" + configFactory.getStringProperty(KAFKA_AUTHORIZATION_PASSWORD) + "\";"
            );
        }
        AdminClient adminClient = AdminClient.create(props);
        return new KafkaTopicMetadataFetcher(adminClient, configFactory);
    }
}
