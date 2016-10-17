package pl.allegro.tech.hermes.integration;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.integration.env.ConsumersStarter;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.assertj.core.api.Assertions.assertThat;

public class MultipleKafkaTest extends IntegrationTest {

    private static final int FRONTEND_PORT = Ports.nextAvailable();
    private static final String FRONTEND_URL = "http://localhost:" + FRONTEND_PORT + "/";

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
        this.publisher = new HermesPublisher(FRONTEND_URL);
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldPublishAndConsumeThroughSecondaryKafka() throws Exception {
        // given
        Topic topic = operations.buildTopic("secondaryKafka", "topic");
        Subscription subscription = operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        wait.untilSubscriptionIsActivated(topic, subscription.getName());
        wait.waitUntilConsumerMetadataAvailable(subscription, "localhost", SECONDARY_KAFKA_PORT);
        remoteService.expectMessages("message");

        // when
        Response response = publisher.publish("secondaryKafka.topic", "message");

        // then
        assertThat(response.getStatusInfo().getFamily()).isEqualTo(SUCCESSFUL);
        remoteService.waitUntilReceived();
    }

    private ConsumersStarter setupConsumers() throws Exception {
        ConsumersStarter consumers = new ConsumersStarter();
        consumers.overrideProperty(Configs.KAFKA_BROKER_LIST, SECONDARY_KAFKA_CONNECT);
        consumers.overrideProperty(Configs.KAFKA_ZOOKEEPER_CONNECT_STRING, SECONDARY_ZK_KAFKA_CONNECT);
        consumers.overrideProperty(Configs.KAFKA_CLUSTER_NAME, SECONDARY_KAFKA_CLUSTER_NAME);
        consumers.overrideProperty(Configs.CONSUMER_HEALTH_CHECK_PORT, 7454);
        consumers.overrideProperty(Configs.METRICS_GRAPHITE_REPORTER, false);
        consumers.overrideProperty(Configs.METRICS_ZOOKEEPER_REPORTER, false);

        consumers.start();

        return consumers;
    }

    private FrontendStarter setupFrontend() throws Exception {
        FrontendStarter frontend = new FrontendStarter(FRONTEND_PORT, false);
        frontend.overrideProperty(Configs.FRONTEND_PORT, FRONTEND_PORT);
        frontend.overrideProperty(Configs.FRONTEND_HTTP2_ENABLED, false);
        frontend.overrideProperty(Configs.FRONTEND_SSL_ENABLED, false);
        frontend.overrideProperty(Configs.KAFKA_BROKER_LIST, SECONDARY_KAFKA_CONNECT);
        frontend.overrideProperty(Configs.KAFKA_ZOOKEEPER_CONNECT_STRING, SECONDARY_ZK_KAFKA_CONNECT);
        frontend.overrideProperty(Configs.KAFKA_CLUSTER_NAME, SECONDARY_KAFKA_CLUSTER_NAME);
        frontend.overrideProperty(Configs.METRICS_GRAPHITE_REPORTER, false);
        frontend.overrideProperty(Configs.METRICS_ZOOKEEPER_REPORTER, false);

        frontend.start();

        return frontend;
    }

}
