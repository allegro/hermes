package pl.allegro.tech.hermes.integration.setup;

import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.endpoint.JerseyClientFactory;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.OK;

public class HermesFrontendInstance {
    private final String frontendUrl;
    private final FrontendStarter frontend;
    private final HermesPublisher publisher;

    HermesFrontendInstance(int port, FrontendStarter frontend) {
        this.frontendUrl = "http://localhost:" + port + "/";
        this.frontend = frontend;
        this.publisher = new HermesPublisher(frontendUrl);
    }

    public static Starter starter() {
        return new Starter();
    }

    public HermesPublisher operations() {
        return publisher;
    }

    public boolean isHealthy() {
        return checkStatus("health");
    }

    public boolean isReady() {
        return checkStatus("ready");
    }

    private boolean checkStatus(String name) {
        Response response = JerseyClientFactory.create().target(frontendUrl).path("status").path(name).request().get();
        return response.getStatus() == OK.getStatusCode();
    }

    public void stop() {
        try {
            frontend.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Starter {
        private final int port = Ports.nextAvailable();
//        private final FrontendStarter frontend = new FrontendStarter(port, false);
        private final FrontendStarter frontend = FrontendStarter.withCommonIntegrationTestConfig(port, false);

        public Starter() {
//            frontend.overrideProperty(Configs.FRONTEND_PORT, port);//TODO?
            frontend.overrideProperty(Configs.FRONTEND_HTTP2_ENABLED, false);
//            frontend.overrideProperty(Configs.FRONTEND_SSL_ENABLED, false);//TODO?
            frontend.overrideProperty(Configs.METRICS_GRAPHITE_REPORTER, false);
            frontend.overrideProperty(Configs.METRICS_ZOOKEEPER_REPORTER, false);
            frontend.overrideProperty(Configs.MESSAGES_LOCAL_STORAGE_ENABLED, false);
            frontend.overrideProperty(Configs.FRONTEND_READINESS_CHECK_ENABLED, true);
            frontend.overrideProperty(Configs.KAFKA_AUTHORIZATION_ENABLED, false);
        }

        public Starter metadataMaxAgeInSeconds(int seconds) {
            frontend.overrideProperty(Configs.KAFKA_PRODUCER_METADATA_MAX_AGE, seconds * 1000);
            return this;
        }

        public Starter readinessCheckIntervalInSeconds(int seconds) {
            frontend.overrideProperty(Configs.FRONTEND_READINESS_CHECK_INTERVAL_SECONDS, seconds);
            return this;
        }

        public Starter zookeeperConnectionString(String connectionString) {
            frontend.overrideProperty(Configs.ZOOKEEPER_CONNECT_STRING, connectionString);
            return this;
        }

        public Starter kafkaConnectionString(String connectionString) {
            frontend.overrideProperty(Configs.KAFKA_BROKER_LIST, connectionString);
            return this;
        }

        public HermesFrontendInstance start() {
            try {
                frontend.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return new HermesFrontendInstance(port, frontend);
        }
    }
}
