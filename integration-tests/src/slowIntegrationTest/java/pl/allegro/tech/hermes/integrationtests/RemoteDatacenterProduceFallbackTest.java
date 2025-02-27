package pl.allegro.tech.hermes.integrationtests;

import static pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider.DEFAULT_DC_NAME;
import static pl.allegro.tech.hermes.integrationtests.assertions.HermesAssertions.assertThatMetrics;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
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
  private static final HermesDatacenter dc3 = new HermesDatacenter();

  private static HermesManagementTestApp management;
  private static HermesInitHelper initHelper;
  private static HermesFrontendTestApp frontendDC1;
  private static HermesFrontendTestApp frontendDC2;
  private static HermesConsumersTestApp consumerDC1;
  private static HermesConsumersTestApp consumerDC2;
  private static HermesConsumersTestApp consumerDC3;

  private static HermesTestClient DC1;
  private static HermesTestClient DC2;
  private static final String REMOTE_DC2 = "dc2";
  private static final String REMOTE_DC3 = "dc3";

  private static final Map<String, Pair<KafkaContainerCluster, List<String>>> kafkaConfiguration =
      Map.of(
          "dc",
          Pair.of(dc1.kafka, List.of(REMOTE_DC2)),
          REMOTE_DC2,
          Pair.of(dc2.kafka, List.of(DEFAULT_DC_NAME, REMOTE_DC3)),
          REMOTE_DC3,
          Pair.of(dc3.kafka, List.of(DEFAULT_DC_NAME, REMOTE_DC2)));

  @BeforeAll
  public static void setup() {
    Stream.of(dc1, dc2, dc3).parallel().forEach(HermesDatacenter::startKafkaAndZookeeper);
    schemaRegistry.start();
    management =
        new HermesManagementTestApp(
            Map.of(
                DEFAULT_DC_NAME,
                dc1.hermesZookeeper,
                REMOTE_DC2,
                dc2.hermesZookeeper,
                REMOTE_DC3,
                dc3.hermesZookeeper),
            Map.of(DEFAULT_DC_NAME, dc1.kafka, REMOTE_DC2, dc2.kafka, REMOTE_DC3, dc3.kafka),
            schemaRegistry);
    management.start();
    frontendDC1 =
        new HermesFrontendTestApp(dc1.hermesZookeeper, kafkaConfiguration, schemaRegistry);
    frontendDC1.start();

    frontendDC2 =
        new HermesFrontendTestApp(dc1.hermesZookeeper, kafkaConfiguration, schemaRegistry);
    frontendDC2.start();

    consumerDC1 = new HermesConsumersTestApp(dc1.hermesZookeeper, dc1.kafka, schemaRegistry);
    consumerDC1.start();

    consumerDC2 = new HermesConsumersTestApp(dc2.hermesZookeeper, dc2.kafka, schemaRegistry);
    consumerDC2.start();

    consumerDC3 = new HermesConsumersTestApp(dc3.hermesZookeeper, dc3.kafka, schemaRegistry);
    consumerDC3.start();

    DC1 = new HermesTestClient(management.getPort(), frontendDC1.getPort(), consumerDC1.getPort());
    DC2 = new HermesTestClient(management.getPort(), frontendDC2.getPort(), consumerDC1.getPort());
    initHelper = new HermesInitHelper(management.getPort());
  }

  @AfterAll
  public static void clean() {
    management.stop();
    consumerDC2.stop();
    consumerDC3.stop();
    frontendDC1.stop();
    frontendDC2.stop();
    consumerDC1.stop();
    schemaRegistry.stop();
    Stream.of(dc1, dc2, dc3).parallel().forEach(HermesDatacenter::stop);
  }

  @AfterEach
  public void afterEach() {
    Stream.of(dc1, dc2, dc3).forEach(dc -> dc.kafka.restoreConnectionsBetweenBrokersAndClients());
    DC1.setReadiness(DEFAULT_DC_NAME, true);
    DC1.setReadiness(REMOTE_DC2, true);
    DC1.setReadiness(REMOTE_DC3, true);
    DC2.setReadiness(DEFAULT_DC_NAME, true);
    DC2.setReadiness(REMOTE_DC2, true);
    DC2.setReadiness(REMOTE_DC3, true);
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
                      "storageDc", REMOTE_DC2)
                  .withValue(1.0);
              assertRemoteDCSendTotalMetric().withValueGreaterThan(remoteDCInitialSendTotal);
            });
  }

  @Test
  public void shouldNotPublishViaRemoteDCNotListedInConfig() throws InterruptedException {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic =
        initHelper.createTopic(
            topicWithRandomName().withFallbackToRemoteDatacenterEnabled().build());
    initHelper.createSubscription(
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build());

    // when both local and dc2 is not available, and dc3 is not listed in config
    dc1.kafka.cutOffConnectionsBetweenBrokersAndClients();
    dc2.kafka.cutOffConnectionsBetweenBrokersAndClients();

    // and message is published
    TestMessage message = TestMessage.of("key1", "value1");
    DC1.publishUntilStatus(topic.getQualifiedName(), message.body(), 503);

    // wait for a few seconds to ensure no messages are published in the background
    Thread.sleep(5000);

    // then no messages are received
    subscriber.noMessagesReceived();
  }

  @Test
  public void shouldReturn500whenAllDCsAreUnavailable() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic =
        initHelper.createTopic(
            topicWithRandomName().withFallbackToRemoteDatacenterEnabled().build());
    initHelper.createSubscription(
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build());

    // when all dcs are not available
    dc1.kafka.cutOffConnectionsBetweenBrokersAndClients();
    dc2.kafka.cutOffConnectionsBetweenBrokersAndClients();
    dc3.kafka.cutOffConnectionsBetweenBrokersAndClients();

    // and message is published
    TestMessage message = TestMessage.of("key1", "value1");
    DC2.publishUntilStatus(topic.getQualifiedName(), message.body(), 503);

    // then no messages are received
    subscriber.noMessagesReceived();
  }

  @Test
  public void shouldNotFallBackToNotReadyDatacenters() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic =
        initHelper.createTopic(
            topicWithRandomName().withFallbackToRemoteDatacenterEnabled().build());
    initHelper.createSubscription(
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build());

    // when local datacenter is not available and remote is not ready
    dc1.kafka.cutOffConnectionsBetweenBrokersAndClients();
    DC1.setReadiness(REMOTE_DC2, false);

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
                        "storageDc", REMOTE_DC2)
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
        .withLabels("storageDc", REMOTE_DC2, "sender", "failFast");
  }
}
