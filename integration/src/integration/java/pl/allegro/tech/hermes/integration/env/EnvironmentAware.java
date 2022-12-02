package pl.allegro.tech.hermes.integration.env;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class EnvironmentAware {

    private static final String HTTP_ENDPOINT_PORT_NAME = "HTTP_ENDPOINT_PORT";
    private static final String MANAGEMENT_PORT_NAME = "MANAGEMENT_PORT";
    private static final String CONSUMER_PORT_NAME = "CONSUMER_PORT";
    private static final String FRONTEND_PORT_NAME = "FRONTEND_PORT";
    private static final String GRAPHITE_HTTP_SERVER_PORT_NAME = "GRAPHITE_HTTP_SERVER_PORT";
    private static final String GRAPHITE_SERVER_PORT_NAME = "GRAPHITE_SERVER_PORT";
    private static final String OAUTH_SERVER_PORT_NAME = "OAUTH_SERVER_PORT";
    private static final String AUDIT_EVENT_PORT_NAME = "AUDIT_EVENT_PORT";
    private static final String CROWD_SERVICE_PORT_NAME = "CROWD_SERVICE_PORT";
    private static final String HORNET_MQ_SUBDIR = "HORNET_MQ_SUBDIR";
    private static final String HORNET_MQ_NETTY_PORT_NAME = "HORNET_MQ_NETTY_PORT";


    static {
        int basePort = (Integer.parseInt(System.getProperty("org.gradle.test.worker", "1"))) % 100 + 20000;


        List<PortMapping> portMappings = Stream.of(
                new PortMapping(HTTP_ENDPOINT_PORT_NAME, 10),
                new PortMapping(MANAGEMENT_PORT_NAME, 20),
                new PortMapping(CONSUMER_PORT_NAME, 30),
                new PortMapping(FRONTEND_PORT_NAME, 40),
                new PortMapping(GRAPHITE_HTTP_SERVER_PORT_NAME, 50),
                new PortMapping(GRAPHITE_SERVER_PORT_NAME, 60),
                new PortMapping(OAUTH_SERVER_PORT_NAME, 70),
                new PortMapping(AUDIT_EVENT_PORT_NAME, 80),
                new PortMapping(CROWD_SERVICE_PORT_NAME, 90),
                new PortMapping(HORNET_MQ_NETTY_PORT_NAME, 95)
        ).map(portMapping -> portMapping.shift(basePort)).collect(Collectors.toList());


        portMappings.forEach(portMapping -> System.setProperty(portMapping.envVariableName, String.valueOf(portMapping.port)));

        System.setProperty(HORNET_MQ_SUBDIR, String.valueOf(basePort));
    }

    private static int portOf(String name) {
        return Integer.parseInt(System.getProperty(name));
    }

    public static int HTTP_ENDPOINT_PORT = portOf(HTTP_ENDPOINT_PORT_NAME);

    public static int MANAGEMENT_PORT = portOf(MANAGEMENT_PORT_NAME);

    public static int CONSUMER_PORT = portOf(CONSUMER_PORT_NAME);

    public static String HTTP_ENDPOINT_URL = "http://localhost:" + HTTP_ENDPOINT_PORT + "/";

    public static String GOOGLE_PUBSUB_ENDPOINT_URL = "googlepubsub://pubsub.googleapis.com:443/projects/test-project/topics/test-topic";

    public static String GOOGLE_PUBSUB_PROJECT_ID = "test-project";

    public static String GOOGLE_PUBSUB_TOPIC_ID = "test-topic";

    public static String GOOGLE_PUBSUB_SUBSCRIPTION_ID = "test-subscription";

    public static int FRONTEND_PORT = portOf(FRONTEND_PORT_NAME);

    public static String FRONTEND_URL = "http://localhost:" + FRONTEND_PORT + "/";

    public static String FRONTEND_TOPICS_ENDPOINT = FRONTEND_URL + "topics";

    public static String MANAGEMENT_ENDPOINT_URL = "http://localhost:" + MANAGEMENT_PORT + "/";

    public static String CONSUMER_ENDPOINT_URL = "http://localhost:" + CONSUMER_PORT + "/";

    public static int GRAPHITE_HTTP_SERVER_PORT = portOf(GRAPHITE_HTTP_SERVER_PORT_NAME);

    public static int GRAPHITE_SERVER_PORT = portOf(GRAPHITE_SERVER_PORT_NAME);

    public static int OAUTH_SERVER_PORT = portOf(OAUTH_SERVER_PORT_NAME);

    public static int AUDIT_EVENT_PORT = portOf(AUDIT_EVENT_PORT_NAME);

    public static String PRIMARY_KAFKA_CLUSTER_NAME = "primary-dc";

    public static String SECONDARY_KAFKA_CLUSTER_NAME = "secondary";

    public static String KAFKA_NAMESPACE = "";

    public static int CROWD_SERVICE_PORT = portOf(CROWD_SERVICE_PORT_NAME);

    public static int HORNET_MQ_NETTY_PORT = portOf(HORNET_MQ_NETTY_PORT_NAME);

    private static class PortMapping {
        private final String envVariableName;
        private final int port;

        public PortMapping(String envVariableName, int port) {
            this.envVariableName = envVariableName;
            this.port = port;
        }

        public PortMapping shift(int value) {
            return new PortMapping(envVariableName, port + value);
        }


    }


}
