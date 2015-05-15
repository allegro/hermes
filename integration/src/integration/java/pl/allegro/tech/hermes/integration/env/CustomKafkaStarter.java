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

        return properties;
    }
}
