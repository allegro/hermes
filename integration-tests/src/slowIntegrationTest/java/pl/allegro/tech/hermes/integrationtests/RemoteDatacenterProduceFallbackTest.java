package pl.allegro.tech.hermes.integrationtests;

import static pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider.DEFAULT_DC_NAME;
import static pl.allegro.tech.hermes.integrationtests.assertions.HermesAssertions.assertThatMetrics;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy.ChaosMode;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy.ChaosPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.assertions.PrometheusMetricsAssertion;
import pl.allegro.tech.hermes.integrationtests.setup.HermesConsumersTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementTestApp;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.client.integration.HermesInitHelper;
import pl.allegro.tech.hermes.test.helper.client.integration.HermesTestClient;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class RemoteDatacenterProduceFallbackTest {

  @RegisterExtension
  public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

  private static final ConfluentSchemaRegistryContainer schemaRegistry =
      new ConfluentSchemaRegistryContainer();
  private static final HermesDatacenter dc1 = new HermesDatacenter();
  private static final HermesDatacenter dc2 = new HermesDatacenter();

  private static HermesManagementTestApp management;
  private static HermesInitHelper initHelper;
  private static HermesFrontendTestApp frontendDC1;
  private static HermesConsumersTestApp consumerDC1;
  private static HermesConsumersTestApp consumerDC2;

  private static HermesTestClient DC1;
  private static final String REMOTE_DC_NAME = "dc2";

  @BeforeAll
  public static void setup() {
    Stream.of(dc1, dc2).parallel().forEach(HermesDatacenter::startKafkaAndZookeeper);
    schemaRegistry.start();
    management =
        new HermesManagementTestApp(
            Map.of(DEFAULT_DC_NAME, dc1.hermesZookeeper, REMOTE_DC_NAME, dc2.hermesZookeeper),
            Map.of(DEFAULT_DC_NAME, dc1.kafka, REMOTE_DC_NAME, dc2.kafka),
            schemaRegistry);
    management.start();
    frontendDC1 =
        new HermesFrontendTestApp(
            dc1.hermesZookeeper,
            Map.of("dc", dc1.kafka, REMOTE_DC_NAME, dc2.kafka),
            schemaRegistry);
    frontendDC1.start();

    consumerDC1 = new HermesConsumersTestApp(dc1.hermesZookeeper, dc1.kafka, schemaRegistry);
    consumerDC1.start();

    consumerDC2 = new HermesConsumersTestApp(dc2.hermesZookeeper, dc2.kafka, schemaRegistry);
    consumerDC2.start();

    DC1 = new HermesTestClient(management.getPort(), frontendDC1.getPort(), consumerDC1.getPort());
    initHelper = new HermesInitHelper(management.getPort());
  }

  @AfterAll
  public static void clean() {
    management.stop();
    consumerDC2.stop();
    frontendDC1.stop();
    consumerDC1.stop();
    schemaRegistry.stop();
    Stream.of(dc1, dc2).parallel().forEach(HermesDatacenter::stop);
  }

  @AfterEach
  public void afterEach() {
    Stream.of(dc1, dc2).forEach(dc -> dc.kafka.restoreConnectionsBetweenBrokersAndClients());
    DC1.setReadiness(DEFAULT_DC_NAME, true);
    DC1.setReadiness(REMOTE_DC_NAME, true);
  }

  @Test
  public void shouldPublishAndConsumeViaRemoteDCWhenLocalKafkaIsUnavailable() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic =
        initHelper.createTopic(
            topicWithRandomName().withFallbackToRemoteDatacenterEnabled().build());
    initHelper.createSubscription(
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build());

    double remoteDCInitialSendTotal = assertRemoteDCSendTotalMetric().withInitialValue();

    // when dc1 is not available
    dc1.kafka.cutOffConnectionsBetweenBrokersAndClients();

    // and message is published to dc1
    TestMessage message = TestMessage.of("key1", "value1");
    DC1.publishUntilSuccess(topic.getQualifiedName(), message.body());

    // then message is received in dc2
    subscriber.waitUntilReceived(message.body());

    // and metrics that message was published to remote dc is incremented
    DC1.getFrontendMetrics()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .value(
            (body) -> {
              assertThatMetrics(body)
                  .contains("hermes_frontend_topic_published_total")
                  .withLabels(
                      "group", topic.getName().getGroupName(),
                      "topic", topic.getName().getName(),
                      "storageDc", REMOTE_DC_NAME)
                  .withValue(1.0);
              assertRemoteDCSendTotalMetric().withValueGreaterThan(remoteDCInitialSendTotal);
            });
  }

  @Test
  public void shouldReturn500whenBothDCsAreUnavailable() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic =
        initHelper.createTopic(
            topicWithRandomName().withFallbackToRemoteDatacenterEnabled().build());
    initHelper.createSubscription(
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build());

    // when both dcs are not available
    dc1.kafka.cutOffConnectionsBetweenBrokersAndClients();
    dc2.kafka.cutOffConnectionsBetweenBrokersAndClients();

    // and message is published
    TestMessage message = TestMessage.of("key1", "value1");
    DC1.publishUntilStatus(topic.getQualifiedName(), message.body(), 503);

    // then no messages are received
    subscriber.noMessagesReceived();
  }

  @Test
  public void shouldNotFallBackToNotReadyDatacenter() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic =
        initHelper.createTopic(
            topicWithRandomName().withFallbackToRemoteDatacenterEnabled().build());
    initHelper.createSubscription(
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build());

    // when local datacenter is not available and remote is not ready
    dc1.kafka.cutOffConnectionsBetweenBrokersAndClients();
    DC1.setReadiness(REMOTE_DC_NAME, false);

    // and message is published
    TestMessage message = TestMessage.of("key1", "value1");
    DC1.publishUntilStatus(topic.getQualifiedName(), message.body(), 503);

    // then no messages are received
    subscriber.noMessagesReceived();
  }

  @Test
  public void shouldPublishAndConsumeViaRemoteDCWhenChaosExperimentIsEnabledForLocalKafka() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();

    Topic topic =
        initHelper.createTopic(
            topicWithRandomName()
                .withFallbackToRemoteDatacenterEnabled()
                .withPublishingChaosPolicy(completeWithErrorForDatacenter(DEFAULT_DC_NAME))
                .build());
    initHelper.createSubscription(
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build());

    // and message is published to dc1
    TestMessage message = TestMessage.of("key1", "value1");
    DC1.publishUntilSuccess(topic.getQualifiedName(), message.body());

    // then message is received in dc2
    subscriber.waitUntilReceived(message.body());

    // and metrics that message was published to remote dc is incremented
    DC1.getFrontendMetrics()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .value(
            (body) ->
                assertThatMetrics(body)
                    .contains("hermes_frontend_topic_published_total")
                    .withLabels(
                        "group", topic.getName().getGroupName(),
                        "topic", topic.getName().getName(),
                        "storageDc", REMOTE_DC_NAME)
                    .withValue(1.0));
  }

  @Test
  public void shouldReturnErrorWhenChaosExperimentIsEnabledForAllDatacenters() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();

    Topic topic =
        initHelper.createTopic(
            topicWithRandomName()
                .withFallbackToRemoteDatacenterEnabled()
                .withPublishingChaosPolicy(completeWithErrorForAllDatacenters())
                .build());
    initHelper.createSubscription(
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build());
    TestMessage message = TestMessage.of("key1", "value1");

    // when
    DC1.publishUntilStatus(topic.getQualifiedName(), message.body(), 500);

    // then
    subscriber.noMessagesReceived();
  }

  private static PublishingChaosPolicy completeWithErrorForAllDatacenters() {
    int delayFrom = 100;
    int delayTo = 200;
    int probability = 100;
    boolean completeWithError = true;
    return new PublishingChaosPolicy(
        ChaosMode.GLOBAL,
        new ChaosPolicy(probability, delayFrom, delayTo, completeWithError),
        null);
  }

  private static PublishingChaosPolicy completeWithErrorForDatacenter(String datacenter) {
    int delayFrom = 100;
    int delayTo = 200;
    int probability = 100;
    boolean completeWithError = true;
    return new PublishingChaosPolicy(
        ChaosMode.DATACENTER,
        null,
        Map.of(datacenter, new ChaosPolicy(probability, delayFrom, delayTo, completeWithError)));
  }

  private static class HermesDatacenter {

    private final ZookeeperContainer hermesZookeeper = new ZookeeperContainer("HermesZookeeper");
    private final KafkaContainerCluster kafka = new KafkaContainerCluster(1);

    public HermesDatacenter() {
      schemaRegistry.withKafkaCluster(kafka);
    }

    void startKafkaAndZookeeper() {
      Stream.of(hermesZookeeper, kafka).parallel().forEach(Startable::start);
    }

    void stop() {
      Stream.of(hermesZookeeper, kafka).parallel().forEach(Startable::stop);
    }
  }

  PrometheusMetricsAssertion.PrometheusMetricAssertion assertRemoteDCSendTotalMetric() {
    return assertThatMetrics(
            DC1.getFrontendMetrics()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody())
        .contains("hermes_frontend_kafka_producer_ack_leader_record_send_total")
        .withLabels("storageDc", REMOTE_DC_NAME, "sender", "failFast");
  }
}
