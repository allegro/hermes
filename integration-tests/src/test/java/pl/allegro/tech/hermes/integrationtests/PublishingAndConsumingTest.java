package pl.allegro.tech.hermes.integrationtests;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesConsumersInstance;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendInstance;
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementInstance;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.util.stream.Stream;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.integrationtests.HermesAssertions.assertThat;

public class PublishingAndConsumingTest {

    public static final KafkaContainerCluster kafkaCluster = new KafkaContainerCluster(1);

    public static final ZookeeperContainer hermesZookeeperOne = new ZookeeperContainer("ZookeeperContainerOne");
    public static HermesManagementInstance managementStarter;
    public static HermesConsumersInstance consumersStarter = new HermesConsumersInstance();

    public static HermesFrontendInstance frontendStarter = HermesFrontendInstance.withCommonIntegrationTestConfig(18080);
    private final HermesManagementOperations operations = new HermesManagementOperations();
    private final TestPublisher publisher = new TestPublisher();
    private final TestSubscribers subscribers = new TestSubscribers();

    @BeforeAll
    public static void setup() throws Exception {
        Stream.of(kafkaCluster, hermesZookeeperOne)
                .parallel()
                .forEach(Startable::start);
        managementStarter = HermesManagementInstance.starter()
                .port(18082)
                .addKafkaCluster("dc", kafkaCluster.getBootstrapServersForExternalClients())
                .addZookeeperCluster("dc", hermesZookeeperOne.getConnectionString())
                .replicationFactor(kafkaCluster.getAllBrokers().size())
                .uncleanLeaderElectionEnabled(false)
                .start();
        consumersStarter.overrideProperty(
                "consumer.kafka.clusters.[0].brokerList", kafkaCluster.getBootstrapServersForExternalClients()
        );
        consumersStarter.overrideProperty(
                "consumer.zookeeper.clusters.[0].connectionString", hermesZookeeperOne.getConnectionString()
        );
        consumersStarter.start();
        frontendStarter.overrideProperty(
                "frontend.kafka.clusters.[0].brokerList", kafkaCluster.getBootstrapServersForExternalClients()
        );
        frontendStarter.overrideProperty(
                "frontend.zookeeper.clusters.[0].connectionString", hermesZookeeperOne.getConnectionString()
        );
        frontendStarter.start();
    }

    @Test
    public void shouldPublishAndConsumeMessage() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = operations.createRandomTopic();
        operations.createRandomSubscription(topic, subscriber.getEndpoint());
        TestMessage message = TestMessage.of("hello", "world");

        // when
        // TODO: consider publisher.publish(topic, message);
        Response response = publisher.publish(topic.getQualifiedName(), message.body());

        // then
        assertThat(response).hasStatus(CREATED);
        subscriber.waitUntilReceived(message.body());
    }

    @Test
    public void shouldConsumeMessagesOnMultipleSubscriptions() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        Topic topic = operations.createRandomTopic();
        TestSubscriber subscriber1 = subscribers.createSubscriber();
        TestSubscriber subscriber2 = subscribers.createSubscriber();
        operations.createRandomSubscription(topic, subscriber1.getEndpoint());
        operations.createRandomSubscription(topic, subscriber2.getEndpoint());

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        subscriber1.waitUntilReceived(message.body());
        subscriber2.waitUntilReceived(message.body());
    }

    @Test
    public void shouldPassSubscriptionFixedHeaders() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        Topic topic = operations.createRandomTopic();
        TestSubscriber subscriber = subscribers.createSubscriber();
        Subscription subscription = SubscriptionBuilder.subscriptionWithRandomName(topic.getName())
                .withEndpoint(subscriber.getEndpoint())
                .withHeader("MY-HEADER", "myHeader123")
                .build();
        operations.createSubscription(topic, subscription);

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request).hasHeaderValue("MY-HEADER", "myHeader123");
            assertThat(request.getHeader("Hermes-Message-Id")).isNotEmpty();
        });
    }
}
