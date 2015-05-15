package pl.allegro.tech.hermes.integration.env;

import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

public interface EnvironmentAware {

    ConfigFactory CONFIG_FACTORY = new ConfigFactory();

    int ZOOKEEPER_PORT = 14192;

    String ZOOKEEPER_CONNECT_STRING = CONFIG_FACTORY.getStringProperty(Configs.ZOOKEEPER_CONNECT_STRING);

    int HTTP_ENDPOINT_PORT = 18081;

    int MANAGEMENT_PORT = 18082;
    
    String HTTP_ENDPOINT_URL = "http://localhost:" + HTTP_ENDPOINT_PORT + "/";

    int FRONTEND_PORT = CONFIG_FACTORY.getIntProperty(Configs.FRONTEND_PORT);
    
    String FRONTEND_URL = "http://localhost:" + FRONTEND_PORT + "/";

    String CLIENT_FRONTEND_URL = "http://localhost:" + FRONTEND_PORT;

    String FRONTEND_TOPICS_ENDPOINT = FRONTEND_URL + "topics";
    
    String FRONTEND_HEALTH_ENDPOINT = FRONTEND_URL + "status/health";

    String MANAGEMENT_ENDPOINT_URL = "http://localhost:" + MANAGEMENT_PORT + "/";
    
    String ADMIN_PASSWORD = CONFIG_FACTORY.getStringProperty(Configs.ADMIN_PASSWORD);

    int GRAPHITE_HTTP_SERVER_PORT = 18089;

    int GRAPHITE_SERVER_PORT = 18023;

    int OAUTH_SERVER_PORT = 19999;

    String PRIMARY_KAFKA_CLUSTER_NAME = CONFIG_FACTORY.getStringProperty(Configs.KAFKA_CLUSTER_NAME);

    String SECONDARY_KAFKA_CLUSTER_NAME = "secondary";

    int SECONDARY_KAFKA_PORT = 9094;

    String SECONDARY_KAFKA_CONNECT = "localhost:" + SECONDARY_KAFKA_PORT;

    String SECONDARY_ZK_KAFKA_CONNECT = "localhost:14192/secondaryKafka";
}
