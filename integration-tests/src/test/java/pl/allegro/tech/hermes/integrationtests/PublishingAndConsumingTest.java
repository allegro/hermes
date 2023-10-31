package pl.allegro.tech.hermes.integrationtests;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesConsumersInstance;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendInstance;
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementExtension;
import pl.allegro.tech.hermes.integrationtests.setup.KafkaExtension;
import pl.allegro.tech.hermes.integrationtests.setup.ZookeeperExtension;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.integrationtests.HermesAssertions.assertThat;

public class PublishingAndConsumingTest {

    @RegisterExtension
    private static KafkaExtension kafka = new KafkaExtension();

    @Order(1)
    @RegisterExtension
    private static ZookeeperExtension zookeeper = new ZookeeperExtension();

    @Order(2)
    @RegisterExtension
    private static HermesManagementExtension managemet = HermesManagementExtension
            .builder()
            .port(18082)
            .addKafkaCluster("dc", kafka)
            .addZookeeperCluster("dc", zookeeper)
            .uncleanLeaderElectionEnabled(false)
            .build();

    private static HermesEndpoints managementEndpoint = new HermesEndpoints("http://localhost:18082/", "http://localhost:18082/");

    public static HermesConsumersInstance consumersStarter = new HermesConsumersInstance();

    public static HermesFrontendInstance frontendStarter = HermesFrontendInstance.withCommonIntegrationTestConfig(18080);
    private final HermesTestClient hermesTestClient = new HermesTestClient(managementEndpoint, "http://localhost:18080/");
    private final TestSubscribers subscribers = new TestSubscribers();

    @BeforeAll
    public static void setup() throws Exception {
        consumersStarter.overrideProperty(
                "consumer.kafka.clusters.[0].brokerList", kafka.kafkaCluster.getBootstrapServersForExternalClients()
        );
        consumersStarter.overrideProperty(
                "consumer.zookeeper.clusters.[0].connectionString", zookeeper.hermesZookeeperOne.getConnectionString()
        );
        consumersStarter.start();
        frontendStarter.overrideProperty(
                "frontend.kafka.clusters.[0].brokerList", kafka.kafkaCluster.getBootstrapServersForExternalClients()
        );
        frontendStarter.overrideProperty(
                "frontend.zookeeper.clusters.[0].connectionString", zookeeper.hermesZookeeperOne.getConnectionString()
        );
        frontendStarter.start();
    }

    @Test
    public void shouldPublishAndConsumeMessage() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermesTestClient.createRandomTopic();
        hermesTestClient.createRandomSubscription(topic, subscriber.getEndpoint());
        TestMessage message = TestMessage.of("hello", "world");

        // when
        // TODO: consider publisher.publish(topic, message);
        Response response = hermesTestClient.publish(topic.getQualifiedName(), message.body());

        // then
        assertThat(response).hasStatus(CREATED);
        subscriber.waitUntilReceived(message.body());
    }

    @Test
    public void shouldConsumeMessagesOnMultipleSubscriptions() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        Topic topic = hermesTestClient.createRandomTopic();
        TestSubscriber subscriber1 = subscribers.createSubscriber();
        TestSubscriber subscriber2 = subscribers.createSubscriber();
        hermesTestClient.createRandomSubscription(topic, subscriber1.getEndpoint());
        hermesTestClient.createRandomSubscription(topic, subscriber2.getEndpoint());

        // when
        hermesTestClient.publish(topic.getQualifiedName(), message.body());

        // then
        subscriber1.waitUntilReceived(message.body());
        subscriber2.waitUntilReceived(message.body());
    }

    @Test
    public void shouldPassSubscriptionFixedHeaders() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        Topic topic = hermesTestClient.createRandomTopic();
        TestSubscriber subscriber = subscribers.createSubscriber();
        Subscription subscription = SubscriptionBuilder.subscriptionWithRandomName(topic.getName())
                .withEndpoint(subscriber.getEndpoint())
                .withHeader("MY-HEADER", "myHeader123")
                .build();
        hermesTestClient.createSubscription(topic, subscription);

        // when
        hermesTestClient.publish(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request).hasHeaderValue("MY-HEADER", "myHeader123");
            assertThat(request.getHeader("Hermes-Message-Id")).isNotEmpty();
        });
    }
}
