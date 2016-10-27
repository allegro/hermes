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

    int FRONTEND_SSL_PORT = CONFIG_FACTORY.getIntProperty(Configs.FRONTEND_SSL_PORT);

    String CLIENT_FRONTEND_URL = "http://localhost:" + FRONTEND_PORT;

    String FRONTEND_TOPICS_ENDPOINT = FRONTEND_URL + "topics";

    String FRONTEND_HEALTH_ENDPOINT = FRONTEND_URL + "status/health";

    String MANAGEMENT_ENDPOINT_URL = "http://localhost:" + MANAGEMENT_PORT + "/";

    String CONSUMER_ENDPOINT_URL = "http://localhost:" + CONFIG_FACTORY.getIntProperty(Configs.CONSUMER_HEALTH_CHECK_PORT) + "/";

    int GRAPHITE_HTTP_SERVER_PORT = 18089;

    int GRAPHITE_SERVER_PORT = 18023;

    int OAUTH_SERVER_PORT = 19999;

    int SCHEMA_REPO_PORT = 8888;

    String PRIMARY_KAFKA_CLUSTER_NAME = CONFIG_FACTORY.getStringProperty(Configs.KAFKA_CLUSTER_NAME);

    String PRIMARY_ZK_KAFKA_CONNECT = CONFIG_FACTORY.getStringProperty(Configs.KAFKA_ZOOKEEPER_CONNECT_STRING);

    String SECONDARY_KAFKA_CLUSTER_NAME = "secondary";

    int SECONDARY_KAFKA_PORT = 9094;

    String SECONDARY_KAFKA_CONNECT = "localhost:" + SECONDARY_KAFKA_PORT;

    String SECONDARY_ZK_KAFKA_CONNECT = "localhost:14192/secondaryKafka";

    String KAFKA_NAMESPACE = CONFIG_FACTORY.getStringProperty(Configs.KAFKA_NAMESPACE);
}
