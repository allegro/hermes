package pl.allegro.tech.hermes.integration;

import com.google.common.io.Files;
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
import pl.allegro.tech.hermes.integration.setup.HermesManagementInstance;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class MultipleKafkaTest extends IntegrationTest {

    private static final int FRONTEND_PORT = Ports.nextAvailable();
    private static final String FRONTEND_URL = "http://localhost:" + FRONTEND_PORT + "/";

    private HermesManagementInstance hermesManagement;
    private HermesPublisher publisher;
    private RemoteServiceEndpoint remoteService;
    private ConsumersStarter consumersStarter;
    private FrontendStarter frontendStarter;

    @BeforeClass
    public void setupEnvironment() throws Exception {
        hermesManagement = HermesManagementInstance.starter()
                .addKafkaCluster(DC1, kafkaClusterOne.getBootstrapServersForExternalClients())
                .addKafkaCluster(DC2, kafkaClusterTwo.getBootstrapServersForExternalClients())
                .addZookeeperCluster(DC1, hermesZookeeperOne.getConnectionString())
                .addZookeeperCluster(DC2, hermesZookeeperTwo.getConnectionString())
                .replicationFactor(kafkaClusterOne.getAllBrokers().size())
                .uncleanLeaderElectionEnabled(false)
                .start();
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
        Topic topic = hermesManagement.operations().buildTopic(randomTopic("secondaryKafka", "topic").build());
        Subscription subscription = hermesManagement.operations().createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        wait.untilSubscriptionIsActivated(topic, subscription.getName());
        remoteService.expectMessages("message");

        // when
        Response response = publisher.publish(topic.getQualifiedName(), "message");

        // then
        assertThat(response.getStatusInfo().getFamily()).isEqualTo(SUCCESSFUL);
        remoteService.waitUntilReceived();
    }

    private ConsumersStarter setupConsumers() throws Exception {
        ConsumersStarter consumers = new ConsumersStarter();
        consumers.overrideProperty(Configs.KAFKA_AUTHORIZATION_ENABLED, false);
        consumers.overrideProperty(Configs.KAFKA_BROKER_LIST, kafkaClusterTwo.getBootstrapServersForExternalClients());
        consumers.overrideProperty(Configs.ZOOKEEPER_CONNECT_STRING, hermesZookeeperTwo.getConnectionString());
        consumers.overrideProperty(Configs.SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());
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
        frontend.overrideProperty(Configs.KAFKA_AUTHORIZATION_ENABLED, false);
        frontend.overrideProperty(Configs.KAFKA_BROKER_LIST, kafkaClusterTwo.getBootstrapServersForExternalClients());
        frontend.overrideProperty(Configs.ZOOKEEPER_CONNECT_STRING, hermesZookeeperTwo.getConnectionString());
        frontend.overrideProperty(Configs.SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());
        frontend.overrideProperty(Configs.KAFKA_CLUSTER_NAME, SECONDARY_KAFKA_CLUSTER_NAME);
        frontend.overrideProperty(Configs.METRICS_GRAPHITE_REPORTER, false);
        frontend.overrideProperty(Configs.METRICS_ZOOKEEPER_REPORTER, false);
        frontend.overrideProperty(Configs.MESSAGES_LOCAL_STORAGE_ENABLED, false);
        frontend.overrideProperty(Configs.MESSAGES_LOCAL_STORAGE_DIRECTORY, Files.createTempDir().getAbsolutePath());

        frontend.start();

        return frontend;
    }

}
