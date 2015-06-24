package pl.allegro.tech.hermes.integration;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.integration.env.ConsumersStarter;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.assertj.core.api.Assertions.assertThat;

public class MultipleKafkaTest extends IntegrationTest {

    private final int frontendPort = 6793;
    private final String frontendUrl = "http://localhost:" + frontendPort + "/";

    private HermesPublisher publisher;
    private RemoteServiceEndpoint remoteService;
    private ConsumersStarter consumersStarter;
    private FrontendStarter frontendStarter;

    @BeforeClass
    public void setupEnvironment() throws Exception {
        consumersStarter = setupConsumers();
        frontendStarter = setupFrontend();
    }

    @AfterClass
    public void cleanEnvironment() throws Exception {
        consumersStarter.stop();
        frontendStarter.stop();
    }

    @BeforeMethod
    public void initializeAlways() {
        this.publisher = new HermesPublisher(frontendUrl);
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldPublishAndConsumeThroughSecondaryKafka() throws Exception {
        // given
        operations.buildSubscription("secondaryKafka", "topic", "subscription", HTTP_ENDPOINT_URL);
        remoteService.expectMessages("message");

        // when
        Response response = publisher.publish("secondaryKafka.topic", "message");

        // then
        assertThat(response.getStatusInfo().getFamily()).isEqualTo(SUCCESSFUL);
        remoteService.waitUntilReceived();
    }

    private ConsumersStarter setupConsumers() throws Exception {
        ConsumersStarter consumers = new ConsumersStarter();
        consumers.overrideProperty(Configs.KAFKA_ZOOKEEPER_CONNECT_STRING, SECONDARY_ZK_KAFKA_CONNECT);
        consumers.overrideProperty(Configs.KAFKA_ZOOKEEPER_CONNECT_STRING, SECONDARY_ZK_KAFKA_CONNECT);
        consumers.overrideProperty(Configs.KAFKA_CLUSTER_NAME, SECONDARY_KAFKA_CLUSTER_NAME);
        consumers.overrideProperty(Configs.CONSUMER_HEALTH_CHECK_PORT, 7454);
        consumers.overrideProperty(Configs.METRICS_GRAPHITE_REPORTER, false);
        consumers.overrideProperty(Configs.METRICS_ZOOKEEPER_REPORTER, false);

        consumers.start();

        return consumers;
    }

    private FrontendStarter setupFrontend() throws Exception {
        FrontendStarter frontend = new FrontendStarter(frontendUrl);
        frontend.overrideProperty(Configs.FRONTEND_PORT, frontendPort);
        frontend.overrideProperty(Configs.FRONTEND_HTTP2_ENABLED, false);
        frontend.overrideProperty(Configs.KAFKA_BROKER_LIST, SECONDARY_KAFKA_CONNECT);
        frontend.overrideProperty(Configs.KAFKA_ZOOKEEPER_CONNECT_STRING, SECONDARY_ZK_KAFKA_CONNECT);
        frontend.overrideProperty(Configs.KAFKA_CLUSTER_NAME, SECONDARY_KAFKA_CLUSTER_NAME);
        frontend.overrideProperty(Configs.METRICS_GRAPHITE_REPORTER, false);
        frontend.overrideProperty(Configs.METRICS_ZOOKEEPER_REPORTER, false);

        frontend.start();

        return frontend;
    }

}
