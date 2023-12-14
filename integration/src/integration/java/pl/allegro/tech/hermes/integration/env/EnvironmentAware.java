package pl.allegro.tech.hermes.integration.env;

public interface EnvironmentAware {

    int HTTP_ENDPOINT_PORT = 18081;

    int MANAGEMENT_PORT = 18082;

    int CONSUMER_PORT = 8000;

    String HTTP_ENDPOINT_URL = "http://localhost:" + HTTP_ENDPOINT_PORT + "/";

    int FRONTEND_PORT = 18080;

    String FRONTEND_URL = "http://localhost:" + FRONTEND_PORT + "/";

    String FRONTEND_TOPICS_ENDPOINT = FRONTEND_URL + "topics";

    String MANAGEMENT_ENDPOINT_URL = "http://localhost:" + MANAGEMENT_PORT + "/";

    String CONSUMER_ENDPOINT_URL = "http://localhost:" + CONSUMER_PORT + "/";

    int GRAPHITE_HTTP_SERVER_PORT = 18089;

    int PROMETHEUS_HTTP_SERVER_PORT = 18090;

    int GRAPHITE_SERVER_PORT = 18023;

    int OAUTH_SERVER_PORT = 19999;

    int AUDIT_EVENT_PORT = 19998;

    String PRIMARY_KAFKA_CLUSTER_NAME = "primary-dc";

    String SECONDARY_KAFKA_CLUSTER_NAME = "secondary";

    String KAFKA_NAMESPACE = "itTest";
}
