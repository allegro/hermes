package pl.allegro.tech.hermes.integration.setup;

import pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.endpoint.JerseyClientFactory;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.OK;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_HTTP2_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_READINESS_CHECK_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_READINESS_CHECK_INTERVAL_SECONDS;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_READINESS_CHECK_KAFKA_CHECK_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.METRICS_GRAPHITE_REPORTER_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.METRICS_ZOOKEEPER_REPORTER_ENABLED;

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
        private final FrontendStarter frontend = FrontendStarter.withCommonIntegrationTestConfig(port, false);

        public Starter() {
            frontend.overrideProperty(FRONTEND_HTTP2_ENABLED, false);
            frontend.overrideProperty(METRICS_GRAPHITE_REPORTER_ENABLED, false);
            frontend.overrideProperty(METRICS_ZOOKEEPER_REPORTER_ENABLED, false);
            frontend.overrideProperty(FRONTEND_READINESS_CHECK_ENABLED, true);
        }

        public Starter metadataMaxAgeInSeconds(int seconds) {
            frontend.overrideProperty(FrontendConfigurationProperties.KAFKA_PRODUCER_METADATA_MAX_AGE, seconds + "s");
            return this;
        }

        public Starter readinessCheckIntervalInSeconds(int seconds) {
            frontend.overrideProperty(FRONTEND_READINESS_CHECK_INTERVAL_SECONDS, seconds + "s");
            return this;
        }

        public Starter zookeeperConnectionString(String connectionString) {
            frontend.overrideProperty(FrontendConfigurationProperties.ZOOKEEPER_CONNECTION_STRING, connectionString);
            return this;
        }

        public Starter kafkaConnectionString(String connectionString) {
            frontend.overrideProperty(FrontendConfigurationProperties.KAFKA_BROKER_LIST, connectionString);
            return this;
        }

        public Starter kafkaCheckEnabled() {
            frontend.overrideProperty(FRONTEND_READINESS_CHECK_KAFKA_CHECK_ENABLED, true);
            return this;
        }

        public Starter kafkaCheckDisabled() {
            frontend.overrideProperty(FRONTEND_READINESS_CHECK_KAFKA_CHECK_ENABLED, false);
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
