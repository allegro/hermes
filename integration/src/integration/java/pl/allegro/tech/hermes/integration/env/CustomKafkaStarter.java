package pl.allegro.tech.hermes.integration.env;

import com.google.common.io.Files;
import pl.allegro.tech.hermes.test.helper.environment.KafkaStarter;

import java.util.Properties;

public class CustomKafkaStarter extends KafkaStarter {

    public CustomKafkaStarter(int port, String zkConnect) {
        super(loadConfig(port, zkConnect));
    }

    private static Properties loadConfig(int port, String zkConnect) {
        Properties properties = new Properties();
        properties.setProperty("port", String.valueOf(port));
        properties.setProperty("zookeeper.connect", zkConnect);
        properties.setProperty("broker.id", "0");
        properties.setProperty("log.dirs", Files.createTempDir().getAbsolutePath());
        properties.setProperty("delete.topic.enable", "true");
        properties.setProperty("offsets.topic.replication.factor", "1");
        properties.setProperty("group.initial.rebalance.delay.ms", "0");
        properties.setProperty("sasl.mechanism", "PLAIN");
        properties.setProperty("security.protocol", "SASL_PLAINTEXT");
        properties.setProperty("sasl.kerberos.service.name", "SASL_PLAINTEXT");
        properties.setProperty("sasl.enabled.mechanisms", "PLAIN");
        properties.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required\n"
                        + "username=\"" + "hermes" + "\"\n"
                        + "password=\"" + "alice-secret" + "\";"
        );
        properties.setProperty("allow.everyone.if.no.acl.found", "true");
        properties.setProperty("listeners", "SASL_PLAINTEXT://:" + String.valueOf(port) + ",PLAINTEXT://:" + String.valueOf(port + 1));
        properties.setProperty("advertised.listeners", "SASL_PLAINTEXT://localhost:" + String.valueOf(port) + ",PLAINTEXT://localhost:" + String.valueOf(port + 1));

        return properties;
    }
}
