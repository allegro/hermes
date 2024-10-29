package pl.allegro.tech.hermes.integrationtests;

import static pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider.DEFAULT_DC_NAME;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.util.Map;
import java.util.stream.Stream;
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
import pl.allegro.tech.hermes.test.helper.environment.HermesTestApp;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class MultiDatacenterPublishingAndSubscribingTest {

  @RegisterExtension
  public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

  private static final ConfluentSchemaRegistryContainer schemaRegistry =
      new ConfluentSchemaRegistryContainer();
  private static final HermesDatacenter dc1 = new HermesDatacenter();
  private static final HermesDatacenter dc2 = new HermesDatacenter();

  private static HermesManagementTestApp management;
  private static HermesInitHelper initHelper;

  @BeforeAll
  public static void setup() {
    Stream.of(dc1, dc2).parallel().forEach(HermesDatacenter::startKafkaAndZookeeper);
    schemaRegistry.start();
    management =
        new HermesManagementTestApp(
            Map.of(DEFAULT_DC_NAME, dc1.hermesZookeeper, "dc2", dc2.hermesZookeeper),
            Map.of(DEFAULT_DC_NAME, dc1.kafka, "dc2", dc2.kafka),
            schemaRegistry);
    management.start();
    initHelper = new HermesInitHelper(management.getPort());
    dc1.startConsumersAndFrontend();
    dc2.startConsumersAndFrontend();
  }

  @AfterAll
  public static void clean() {
    management.stop();
    Stream.of(dc1, dc2).parallel().forEach(HermesDatacenter::stop);
    schemaRegistry.stop();
  }

  @Test
  public void shouldPublishAndConsumeThroughMultipleDatacenters() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    TestMessage messageDc1 = TestMessage.of("key1", "value1");
    TestMessage messageDc2 = TestMessage.of("key2", "value2");
    Topic topic = initHelper.createTopic(topicWithRandomName().build());
    initHelper.createSubscription(
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build());

    // when
    dc1.api().publishUntilSuccess(topic.getQualifiedName(), messageDc1.body());
    dc2.api().publishUntilSuccess(topic.getQualifiedName(), messageDc2.body());

    // then
    subscriber.waitUntilReceived(messageDc1.body());
    subscriber.waitUntilReceived(messageDc2.body());
  }

  private static class HermesDatacenter {

    private final ZookeeperContainer hermesZookeeper = new ZookeeperContainer("HermesZookeeper");
    private final KafkaContainerCluster kafka = new KafkaContainerCluster(1);
    private final HermesConsumersTestApp consumers =
        new HermesConsumersTestApp(hermesZookeeper, kafka, schemaRegistry);
    private final HermesFrontendTestApp frontend =
        new HermesFrontendTestApp(hermesZookeeper, kafka, schemaRegistry);
    private FrontendTestClient frontendClient;

    public HermesDatacenter() {
      schemaRegistry.withKafkaCluster(kafka);
    }

    void startKafkaAndZookeeper() {
      Stream.of(hermesZookeeper, kafka).parallel().forEach(Startable::start);
    }

    void startConsumersAndFrontend() {
      frontend.start();
      consumers.start();
      frontendClient = new FrontendTestClient(frontend.getPort());
    }

    void stop() {
      Stream.of(consumers, frontend).parallel().forEach(HermesTestApp::stop);
      Stream.of(hermesZookeeper, kafka).parallel().forEach(Startable::stop);
    }

    FrontendTestClient api() {
      return frontendClient;
    }
  }
}
