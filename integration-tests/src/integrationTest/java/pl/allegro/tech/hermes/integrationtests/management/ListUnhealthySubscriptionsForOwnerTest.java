package pl.allegro.tech.hermes.integrationtests.management;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.MonitoringDetails.Severity;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.malfunctioning;
import static pl.allegro.tech.hermes.integrationtests.prometheus.SubscriptionMetrics.subscriptionMetrics;
import static pl.allegro.tech.hermes.integrationtests.prometheus.TopicMetrics.topicMetrics;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.MonitoringDetails;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.UnhealthySubscription;
import pl.allegro.tech.hermes.integrationtests.prometheus.PrometheusExtension;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;

public class ListUnhealthySubscriptionsForOwnerTest {

  @Order(0)
  @RegisterExtension
  public static final PrometheusExtension prometheus = new PrometheusExtension();

  @Order(1)
  @RegisterExtension
  public static final HermesExtension hermes = new HermesExtension().withPrometheus(prometheus);

  @BeforeAll
  static void setup() {
    hermes.clearManagementData();
  }

  @AfterEach
  void cleanup() {
    hermes.clearManagementData();
  }

  @Test
  public void shouldNotListHealthySubscriptions() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription healthySubscription = createSubscriptionOwnedBy("Team A", topic);
    prometheus.stubTopicMetrics(
        topicMetrics(topic.getName()).withRate(100).withDeliveryRate(100).build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(healthySubscription.getQualifiedName()).withRate(100).build());

    // when / then
    hermes
        .api()
        .listUnhealthyForOwner("Team A")
        .expectStatus()
        .isOk()
        .expectBodyList(UnhealthySubscription.class)
        .hasSize(0);
    hermes
        .api()
        .listUnhealthyForOwnerAsPlainText("Team A")
        .expectStatus()
        .isOk()
        .expectBody()
        .isEmpty();
  }

  @Test
  public void shouldReturnOnlyUnhealthySubscriptionOfSingleOwner() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription healthySubscription = createSubscriptionOwnedBy("Team A", topic);
    Subscription unhealthySubscription = createSubscriptionOwnedBy("Team A", topic);
    prometheus.stubTopicMetrics(
        topicMetrics(topic.getName()).withRate(100).withDeliveryRate(50).build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(healthySubscription.getQualifiedName()).withRate(100).build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(unhealthySubscription.getQualifiedName())
            .withRate(50)
            .with500Rate(11)
            .build());

    // when / then
    hermes
        .api()
        .listUnhealthyForOwner("Team A")
        .expectStatus()
        .isOk()
        .expectBodyList(UnhealthySubscription.class)
        .contains(malfunctioningSubscription(unhealthySubscription, 11));
    hermes
        .api()
        .listUnhealthyForOwnerAsPlainText("Team A")
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo(
            "%s - Consuming service returns a lot of 5xx codes for subscription %s, currently 11 5xx/s"
                .formatted(
                    unhealthySubscription.getName(),
                    unhealthySubscription.getQualifiedName().toString()));
  }

  @Test
  public void shouldReportAllUnhealthySubscriptions() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription healthySubscription = createSubscriptionOwnedBy("Team A", topic);
    Subscription unhealthySubscription = createSubscriptionOwnedBy("Team A", topic);
    prometheus.stubTopicMetrics(
        topicMetrics(topic.getName()).withRate(100).withDeliveryRate(50).build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(healthySubscription.getQualifiedName()).withRate(100).build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(unhealthySubscription.getQualifiedName())
            .withRate(50)
            .with500Rate(11)
            .build());

    // when / then
    hermes
        .api()
        .listUnhealthy()
        .expectStatus()
        .isOk()
        .expectBodyList(UnhealthySubscription.class)
        .contains(malfunctioningSubscription(unhealthySubscription, 11));
    hermes
        .api()
        .listUnhealthyAsPlainText()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo(
            "%s - Consuming service returns a lot of 5xx codes for subscription %s, currently 11 5xx/s"
                .formatted(
                    unhealthySubscription.getName(),
                    unhealthySubscription.getQualifiedName().toString()));
  }

  @Test
  public void shouldReturnUnhealthySubscriptionsFilteredByTopic() {
    // given
    Topic topic1 = hermes.initHelper().createTopic(topicWithRandomName().build());
    Topic topic2 = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription unhealthySubscriptionTopic1 = createSubscriptionOwnedBy("Team A", topic1);
    Subscription unhealthySubscriptionTopic2 = createSubscriptionOwnedBy("Team A", topic2);
    prometheus.stubTopicMetrics(
        topicMetrics(topic1.getName()).withRate(100).withDeliveryRate(50).build());
    prometheus.stubTopicMetrics(
        topicMetrics(topic2.getName()).withRate(100).withDeliveryRate(50).build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(unhealthySubscriptionTopic1.getQualifiedName())
            .withRate(50)
            .with500Rate(11)
            .build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(unhealthySubscriptionTopic2.getQualifiedName())
            .withRate(50)
            .with500Rate(11)
            .build());

    // when / then
    hermes
        .api()
        .listUnhealthyForTopic(topic2.getQualifiedName())
        .expectStatus()
        .isOk()
        .expectBodyList(UnhealthySubscription.class)
        .contains(malfunctioningSubscription(unhealthySubscriptionTopic2, 11));
    hermes
        .api()
        .listUnhealthyForTopicAsPlainText(topic2.getQualifiedName())
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo(
            "%s - Consuming service returns a lot of 5xx codes for subscription %s, currently 11 5xx/s"
                .formatted(
                    unhealthySubscriptionTopic2.getName(),
                    unhealthySubscriptionTopic2.getQualifiedName().toString()));
  }

  @Test
  public void shouldReturnSpecificUnhealthySubscription() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription healthySubscription = createSubscriptionOwnedBy("Team A", topic);
    Subscription unhealthySubscription = createSubscriptionOwnedBy("Team A", topic);
    prometheus.stubTopicMetrics(
        topicMetrics(topic.getName()).withRate(100).withDeliveryRate(50).build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(healthySubscription.getQualifiedName()).withRate(100).build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(unhealthySubscription.getQualifiedName())
            .withRate(50)
            .with500Rate(11)
            .build());

    // when / then
    hermes
        .api()
        .listUnhealthyForSubscription(topic.getQualifiedName(), healthySubscription.getName())
        .expectStatus()
        .isOk()
        .expectBodyList(UnhealthySubscription.class)
        .hasSize(0);
    hermes
        .api()
        .listUnhealthyForSubscriptionAsPlainText(
            topic.getQualifiedName(), healthySubscription.getName())
        .expectStatus()
        .isOk()
        .expectBody()
        .isEmpty();
    hermes
        .api()
        .listUnhealthyForSubscription(topic.getQualifiedName(), unhealthySubscription.getName())
        .expectStatus()
        .isOk()
        .expectBodyList(UnhealthySubscription.class)
        .contains(malfunctioningSubscription(unhealthySubscription, 11));
    hermes
        .api()
        .listUnhealthyForSubscriptionAsPlainText(
            topic.getQualifiedName(), unhealthySubscription.getName())
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo(
            "%s - Consuming service returns a lot of 5xx codes for subscription %s, currently 11 5xx/s"
                .formatted(
                    unhealthySubscription.getName(),
                    unhealthySubscription.getQualifiedName().toString()));
  }

  @Test
  public void shouldReportUnhealthySubscriptionsDisrespectingSeverity() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription1 = createSubscriptionOwnedBy("Team A", topic, Severity.CRITICAL);
    Subscription subscription2 = createSubscriptionOwnedBy("Team A", topic, Severity.IMPORTANT);
    Subscription subscription3 = createSubscriptionOwnedBy("Team A", topic, Severity.NON_IMPORTANT);
    prometheus.stubTopicMetrics(
        topicMetrics(topic.getName()).withRate(100).withDeliveryRate(50).build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(subscription1.getQualifiedName()).withRate(50).with500Rate(11).build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(subscription2.getQualifiedName()).withRate(50).with500Rate(11).build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(subscription3.getQualifiedName()).withRate(50).with500Rate(11).build());

    // when / then
    hermes
        .api()
        .listUnhealthyForOwner("Team A")
        .expectStatus()
        .isOk()
        .expectBodyList(UnhealthySubscription.class)
        .contains(
            malfunctioningSubscription(subscription1, 11),
            malfunctioningSubscription(subscription2, 11),
            malfunctioningSubscription(subscription3, 11));
    hermes
        .api()
        .listUnhealthyForOwnerAsPlainText("Team A")
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo(
            """
                        %s - Consuming service returns a lot of 5xx codes for subscription %s, currently 11 5xx/s\r
                        %s - Consuming service returns a lot of 5xx codes for subscription %s, currently 11 5xx/s\r
                        %s - Consuming service returns a lot of 5xx codes for subscription %s, currently 11 5xx/s"""
                .formatted(
                    subscription1.getName(), subscription1.getQualifiedName().toString(),
                    subscription2.getName(), subscription2.getQualifiedName().toString(),
                    subscription3.getName(), subscription3.getQualifiedName().toString()));
  }

  @Test
  public void shouldTimeoutUnhealthySubscriptionsRequest() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    createSubscriptionOwnedBy("Team A", topic);
    prometheus.stubDelay(Duration.ofMillis(3000));

    // when
    long start = System.currentTimeMillis();
    WebTestClient.ResponseSpec response = hermes.api().listUnhealthyAsPlainText();
    long end = System.currentTimeMillis();

    // then
    response.expectStatus().isOk().expectBody().isEmpty();
    assertThat(end - start).isLessThan(3000);
  }

  @Test
  public void shouldReportSuspendedSubscriptionAsHealthy() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription unhealthySubscription = createSubscriptionOwnedBy("Team A", topic);
    prometheus.stubTopicMetrics(
        topicMetrics(topic.getName()).withRate(100).withDeliveryRate(50).build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(unhealthySubscription.getQualifiedName())
            .withRate(50)
            .with500Rate(11)
            .build());

    // when
    hermes.api().suspendSubscription(topic, unhealthySubscription.getName());

    // then
    hermes
        .api()
        .listUnhealthy()
        .expectStatus()
        .isOk()
        .expectBodyList(UnhealthySubscription.class)
        .hasSize(0);
    hermes.api().listUnhealthyAsPlainText().expectStatus().isOk().expectBody().isEmpty();
  }

  private Subscription createSubscriptionOwnedBy(String ownerId, Topic topic) {
    return createSubscriptionOwnedBy(ownerId, topic, Severity.IMPORTANT);
  }

  private Subscription createSubscriptionOwnedBy(String ownerId, Topic topic, Severity severity) {
    return hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName())
                .withOwner(new OwnerId("Plaintext", ownerId))
                .withMonitoringDetails(new MonitoringDetails(severity, ""))
                .withEndpoint("http://localhost:8080")
                .build());
  }

  private static UnhealthySubscription malfunctioningSubscription(
      Subscription subscription, double code5xxErrorsRate) {
    return new UnhealthySubscription(
        subscription.getName(),
        subscription.getTopicName().qualifiedName(),
        subscription.getMonitoringDetails().getSeverity(),
        Set.of(malfunctioning(code5xxErrorsRate, subscription.getQualifiedName().toString())));
  }
}
