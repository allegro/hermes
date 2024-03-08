package pl.allegro.tech.hermes.management.config.kafka;

import org.apache.kafka.clients.admin.AdminClient;

import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;

public class AdminClientFactory {

    public static AdminClient brokerAdminClient(KafkaProperties kafkaProperties) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerList());
        props.put(SECURITY_PROTOCOL_CONFIG, DEFAULT_SECURITY_PROTOCOL);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, kafkaProperties.getKafkaServerRequestTimeoutMillis());
        if (kafkaProperties.getAuthentication().isEnabled()) {
            props.put(SASL_MECHANISM, kafkaProperties.getAuthentication().getMechanism());
            props.put(SECURITY_PROTOCOL_CONFIG, kafkaProperties.getAuthentication().getProtocol());
            props.put(SASL_JAAS_CONFIG, kafkaProperties.getAuthentication().getJaasConfig());
        }
        return AdminClient.create(props);
    }
}