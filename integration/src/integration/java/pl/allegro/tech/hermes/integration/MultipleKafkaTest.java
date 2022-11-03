package pl.allegro.tech.hermes.integration;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties;
import pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties;
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
import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.CONSUMER_HEALTH_CHECK_PORT;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_HTTP2_ENABLED;
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
    public void shouldPublishAndConsumeThroughSecondaryKafka() {
        // given
        Topic topic = hermesManagement.operations().buildTopic(randomTopic("secondaryKafka", "topic").build());
        Subscription subscription = hermesManagement.operations().createSubscription(topic, "subscription", remoteService.getUrl());
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
        consumers.overrideProperty(ConsumerConfigurationProperties.KAFKA_AUTHORIZATION_ENABLED, false);
        consumers.overrideProperty(
                ConsumerConfigurationProperties.KAFKA_BROKER_LIST, kafkaClusterTwo.getBootstrapServersForExternalClients()
        );
        consumers.overrideProperty(ConsumerConfigurationProperties.ZOOKEEPER_CONNECTION_STRING, hermesZookeeperTwo.getConnectionString());
        consumers.overrideProperty(ConsumerConfigurationProperties.SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());
        consumers.overrideProperty(ConsumerConfigurationProperties.KAFKA_CLUSTER_NAME, SECONDARY_KAFKA_CLUSTER_NAME);
        consumers.overrideProperty(CONSUMER_HEALTH_CHECK_PORT, 7454);
        consumers.overrideProperty(ConsumerConfigurationProperties.METRICS_GRAPHITE_REPORTER_ENABLED, false);
        consumers.overrideProperty(ConsumerConfigurationProperties.METRICS_ZOOKEEPER_REPORTER_ENABLED, false);

        consumers.start();

        return consumers;
    }

    private FrontendStarter setupFrontend() throws Exception {
        FrontendStarter frontend = FrontendStarter.withCommonIntegrationTestConfig(FRONTEND_PORT, false);
        frontend.overrideProperty(FRONTEND_HTTP2_ENABLED, false);
        frontend.overrideProperty(
                FrontendConfigurationProperties.KAFKA_BROKER_LIST, kafkaClusterTwo.getBootstrapServersForExternalClients()
        );
        frontend.overrideProperty(FrontendConfigurationProperties.ZOOKEEPER_CONNECTION_STRING, hermesZookeeperTwo.getConnectionString());
        frontend.overrideProperty(FrontendConfigurationProperties.SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());
        frontend.overrideProperty(FrontendConfigurationProperties.METRICS_GRAPHITE_REPORTER_ENABLED, false);
        frontend.overrideProperty(FrontendConfigurationProperties.METRICS_ZOOKEEPER_REPORTER_ENABLED, false);

        frontend.start();

        return frontend;
    }
}
