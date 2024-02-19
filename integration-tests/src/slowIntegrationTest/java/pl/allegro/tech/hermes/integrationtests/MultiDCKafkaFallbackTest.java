package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesConsumersTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementTestApp;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.client.integration.FrontendTestClient;
import pl.allegro.tech.hermes.test.helper.client.integration.HermesInitHelper;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.util.Map;
import java.util.stream.Stream;

import static pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider.DEFAULT_DC_NAME;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class MultiDCKafkaFallbackTest {

    @RegisterExtension
    public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

    private static final ConfluentSchemaRegistryContainer schemaRegistry = new ConfluentSchemaRegistryContainer();
    private static final HermesDatacenter dc1 = new HermesDatacenter();
    private static final HermesDatacenter dc2 = new HermesDatacenter();

    private static HermesManagementTestApp management;
    private static HermesInitHelper initHelper;
    private static HermesFrontendTestApp frontendDC1;
    private static HermesConsumersTestApp consumerDC2;

    private static FrontendTestClient DC1;

    @BeforeAll
    public static void setup() {
        Stream.of(dc1, dc2)
                .parallel()
                .forEach(HermesDatacenter::startKafkaAndZookeeper);
        schemaRegistry.start();
        management = new HermesManagementTestApp(
                Map.of(DEFAULT_DC_NAME, dc1.hermesZookeeper, "dc2", dc2.hermesZookeeper),
                Map.of(DEFAULT_DC_NAME, dc1.kafka, "dc2", dc2.kafka),
                schemaRegistry
        );
        management.start();
        frontendDC1 = new HermesFrontendTestApp(dc1.hermesZookeeper,
                Map.of("dc", dc1.kafka, "dc2", dc2.kafka),
                schemaRegistry
        );
        frontendDC1.start();

        consumerDC2 = new HermesConsumersTestApp(dc2.hermesZookeeper, dc2.kafka, schemaRegistry);
        consumerDC2.start();

        DC1 = new FrontendTestClient(frontendDC1.getPort());
        initHelper = new HermesInitHelper(management.getPort());
    }

    @AfterAll
    public static void clean() {
        management.stop();
        Stream.of(dc1, dc2)
                .parallel()
                .forEach(HermesDatacenter::stop);
        schemaRegistry.stop();
        consumerDC2.stop();
        frontendDC1.stop();
    }

    @Test
    public void shouldPublishAndConsumeThroughMultipleDatacenters() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        TestMessage message = TestMessage.of("key1", "value1");
        Topic topic = initHelper.createTopic(topicWithRandomName().build());
        initHelper.createSubscription(
                subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build()
        );

        // when dc1 is not available
        dc1.kafka.cutOffConnectionsBetweenBrokersAndClients();

        // and message is published to dc1
        DC1.publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then message is received in dc2
        subscriber.waitUntilReceived(message.body());
    }

    private static class HermesDatacenter {

        private final ZookeeperContainer hermesZookeeper = new ZookeeperContainer("HermesZookeeper");
        private final KafkaContainerCluster kafka = new KafkaContainerCluster(1);

        public HermesDatacenter() {
            schemaRegistry.withKafkaCluster(kafka);
        }

        void startKafkaAndZookeeper() {
            Stream.of(hermesZookeeper, kafka)
                    .parallel()
                    .forEach(Startable::start);
        }


        void stop() {
            Stream.of(hermesZookeeper, kafka)
                    .parallel()
                    .forEach(Startable::stop);
        }
    }
}
