package pl.allegro.tech.hermes.integrationtests.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.api.ErrorCode.VALIDATION_ERROR;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.api.SubscriptionHealth.Status.NO_DATA;
import static pl.allegro.tech.hermes.api.SubscriptionHealth.Status.UNHEALTHY;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.malfunctioning;
import static pl.allegro.tech.hermes.integrationtests.prometheus.SubscriptionMetrics.subscriptionMetrics;
import static pl.allegro.tech.hermes.integrationtests.prometheus.TopicMetrics.topicMetrics;
import static pl.allegro.tech.hermes.integrationtests.setup.HermesExtension.auditEvents;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.ConsumerGroup;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.DeliveryType;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.TopicPartition;
import pl.allegro.tech.hermes.api.TrackingMode;
import pl.allegro.tech.hermes.api.subscription.metrics.SubscriptionMetricsConfig;
import pl.allegro.tech.hermes.integrationtests.prometheus.PrometheusExtension;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.management.TestSecurityProvider;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class SubscriptionManagementTest {

  @Order(0)
  @RegisterExtension
  public static final PrometheusExtension prometheus = new PrometheusExtension();

  @Order(1)
  @RegisterExtension
  public static final HermesExtension hermes = new HermesExtension().withPrometheus(prometheus);

  @RegisterExtension
  public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

  public static final TestMessage MESSAGE = TestMessage.of("hello", "world");

  public static final PatchData PATCH_DATA =
      patchData()
          .set("endpoint", EndpointAddress.of("http://localhost:7777/topics/test-topic"))
          .build();

  @AfterEach
  public void cleanup() {
    TestSecurityProvider.reset();
  }

  @Test
  public void shouldEmitAuditEventWhenSubscriptionCreated() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

    // when
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

    // then
    assertThat(auditEvents.getLastReceivedRequest().getBodyAsString())
        .contains("CREATED", subscription.getName());
  }

  @Test
  public void shouldReturnSubscription() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

    // when
    Subscription fetchedSubscription =
        hermes.api().getSubscription(topic.getQualifiedName(), subscription.getName());

    // then
    assertThat(fetchedSubscription.getName()).isEqualTo(subscription.getName());
    assertThat(fetchedSubscription.isAutoDeleteWithTopicEnabled())
        .isEqualTo(subscription.isAutoDeleteWithTopicEnabled());
    assertThat(fetchedSubscription.getQualifiedTopicName()).isEqualTo(topic.getQualifiedName());
    assertThat(fetchedSubscription.isTrackingEnabled()).isEqualTo(subscription.isTrackingEnabled());
  }

  @Test
  public void shouldEmitAuditEventWhenSubscriptionRemoved() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

    // when
    hermes.api().deleteSubscription(topic.getQualifiedName(), subscription.getName());

    // then
    assertThat(auditEvents.getLastReceivedRequest().getBodyAsString())
        .contains("REMOVED", subscription.getName());
  }

  @Test
  public void shouldEmitAuditEventWhenSubscriptionEndpointUpdated() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

    // when
    hermes
        .api()
        .updateSubscription(
            topic,
            subscription.getName(),
            patchData().set("endpoint", EndpointAddress.of("http://another-service")).build());

    // then
    assertThat(auditEvents.getLastReceivedRequest().getBodyAsString())
        .contains("UPDATED", subscription.getName());
  }

  @Test
  public void shouldCreateSubscriptionWithActiveStatus() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription = subscriptionWithRandomName(topic.getName()).build();

    // when
    WebTestClient.ResponseSpec response = hermes.api().createSubscription(subscription);

    // then
    response.expectStatus().isCreated();
    hermes.api().waitUntilSubscriptionActivated(topic.getQualifiedName(), subscription.getName());
  }

  @Test
  public void shouldNotRemoveSubscriptionAfterItsRecreation() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

    // when
    WebTestClient.ResponseSpec response = hermes.api().createSubscription(subscription);

    // then
    response.expectStatus().isBadRequest();

    assertThat(
            hermes
                .api()
                .getSubscription(topic.getQualifiedName(), subscription.getName())
                .getName())
        .isEqualTo(subscription.getName());
  }

  @Test
  public void shouldNotCreateSubscriptionWithoutTopic() {
    // given
    Subscription subscription =
        subscriptionWithRandomName(TopicName.fromQualifiedName("pl.group.non-existing")).build();

    // when
    WebTestClient.ResponseSpec response = hermes.api().createSubscription(subscription);

    // then
    response.expectStatus().isNotFound();
  }

  @Test
  public void shouldSuspendSubscription() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().suspendSubscription(topic, subscription.getName());

    // then
    response.expectStatus().isOk();
    hermes.api().waitUntilSubscriptionSuspended(topic.getQualifiedName(), subscription.getName());
  }

  @Test
  public void shouldUpdateSubscriptionEndpoint() {
    // given
    EndpointAddress updatedEndpoint = EndpointAddress.of("http://another-service-endpoint");
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

    // when
    WebTestClient.ResponseSpec response =
        hermes
            .api()
            .updateSubscription(
                topic,
                subscription.getName(),
                patchData().set("endpoint", updatedEndpoint).build());

    // then
    response.expectStatus().isOk();
    waitAtMost(Duration.ofSeconds(10))
        .until(
            () ->
                hermes
                    .api()
                    .getSubscriptionResponse(topic.getQualifiedName(), subscription.getName())
                    .expectStatus()
                    .is2xxSuccessful()
                    .expectBody(Subscription.class)
                    .returnResult()
                    .getResponseBody()
                    .getEndpoint()
                    .equals(updatedEndpoint));
  }

  @Test
  public void shouldUpdateSubscriptionPolicy() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());
    PatchData patchData =
        patchData()
            .set(
                "subscriptionPolicy",
                ImmutableMap.builder()
                    .put("inflightSize", 100)
                    .put("messageBackoff", 100)
                    .put("messageTtl", 3600)
                    .put("rate", 300)
                    .put("requestTimeout", 1000)
                    .put("socketTimeout", 3000)
                    .put("retryClientErrors", false)
                    .put("sendingDelay", 1000)
                    .build())
            .build();

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().updateSubscription(topic, subscription.getName(), patchData);

    // then
    response.expectStatus().isOk();
    SubscriptionPolicy policy =
        hermes
            .api()
            .getSubscription(topic.getQualifiedName(), subscription.getName())
            .getSerialSubscriptionPolicy();
    assertThat(policy.getInflightSize()).isEqualTo(100);
    assertThat(policy.getMessageBackoff()).isEqualTo(100);
    assertThat(policy.getMessageTtl()).isEqualTo(3600);
    assertThat(policy.getRate()).isEqualTo(300);
    assertThat(policy.getRequestTimeout()).isEqualTo(1000);
    assertThat(policy.getSocketTimeout()).isEqualTo(3000);
    assertThat(policy.isRetryClientErrors()).isFalse();
    assertThat(policy.getSendingDelay()).isEqualTo(1000);
  }

  @Test
  public void shouldUpdateMetricsConfiguration() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());
    PatchData patchData =
        patchData()
            .set(
                "metricsConfig",
                Map.of(
                    "messageProcessing",
                    Map.of("enabled", true, "thresholdsMilliseconds", new String[] {"60000"})))
            .build();

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().updateSubscription(topic, subscription.getName(), patchData);

    // then
    response.expectStatus().isOk();
    SubscriptionMetricsConfig metricsConfig =
        hermes
            .api()
            .getSubscription(topic.getQualifiedName(), subscription.getName())
            .getMetricsConfig();
    assertThat(metricsConfig.messageProcessing().enabled()).isTrue();
    assertThat(metricsConfig.messageProcessing().getThresholdsDurations())
        .containsExactly(Duration.ofMillis(60000));
  }

  @Test
  public void shouldUpdateThresholdsMillisecondsToEmptyList() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());
    PatchData patchData =
        patchData()
            .set(
                "metricsConfig",
                ImmutableMap.builder()
                    .put(
                        "messageProcessing",
                        ImmutableMap.builder()
                            .put("enabled", true)
                            .put("thresholdsMilliseconds", new String[] {})
                            .build())
                    .build())
            .build();

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().updateSubscription(topic, subscription.getName(), patchData);

    // then
    response.expectStatus().isOk();
    SubscriptionMetricsConfig metricsConfig =
        hermes
            .api()
            .getSubscription(topic.getQualifiedName(), subscription.getName())
            .getMetricsConfig();
    assertThat(metricsConfig.messageProcessing().thresholdsMilliseconds()).isEmpty();
  }

  @Test
  public void shouldNotUpdateThresholdsMillisecondsToNegativeValue() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());
    PatchData patchData =
        patchData()
            .set(
                "metricsConfig",
                ImmutableMap.builder()
                    .put(
                        "messageProcessing",
                        ImmutableMap.builder()
                            .put("thresholdsMilliseconds", new String[] {"-100"})
                            .build())
                    .build())
            .build();

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().updateSubscription(topic, subscription.getName(), patchData);

    // then
    response.expectStatus().isBadRequest();
    ErrorDescription responseBody =
        Objects.requireNonNull(
            response.expectBody(ErrorDescription.class).returnResult().getResponseBody());
    assertThat(responseBody.getCode()).isEqualTo(VALIDATION_ERROR);
    assertThat(responseBody.getMessage())
        .isEqualTo(
            "Subscription.metricsConfig.messageProcessing.thresholdsMilliseconds[0].<list element> must be greater than 0");
    SubscriptionMetricsConfig metricsConfig =
        hermes
            .api()
            .getSubscription(topic.getQualifiedName(), subscription.getName())
            .getMetricsConfig();
    assertThat(metricsConfig.messageProcessing().thresholdsMilliseconds()).isEmpty();
  }

  @Test
  public void shouldRemoveSubscription() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().deleteSubscription(topic.getQualifiedName(), subscription.getName());

    // then
    response.expectStatus().isOk();
    hermes
        .api()
        .getSubscriptionResponse(topic.getQualifiedName(), subscription.getName())
        .expectStatus()
        .isBadRequest();
  }

  @Test
  public void shouldReturnSubscriptionsThatAreCurrentlyTrackedForGivenTopic() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription trackedSubscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName())
                    .withTrackingMode(TrackingMode.TRACK_ALL)
                    .build());
    hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().listTrackedSubscriptions(topic.getQualifiedName());

    // then
    response.expectStatus().isOk();
    assertThat(response.expectBody(String[].class).returnResult().getResponseBody())
        .containsExactly(trackedSubscription.getName());
  }

  @Test
  public void shouldReturnsTrackedAndNotSuspendedSubscriptionsForGivenTopic() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription trackedSubscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName())
                    .withTrackingMode(TrackingMode.TRACK_ALL)
                    .build());
    Subscription trackedSubscriptionSuspended =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName())
                    .withTrackingMode(TrackingMode.TRACK_ALL)
                    .build());
    hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

    hermes.api().suspendSubscription(topic, trackedSubscriptionSuspended.getName());

    // and
    String query =
        "{\"query\": {\"trackingEnabled\": \"true\", \"state\": {\"ne\": \"SUSPENDED\"}}}";

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().querySubscriptions(topic.getQualifiedName(), query);

    // then
    assertThat(
            Arrays.stream(
                    Objects.requireNonNull(
                        response.expectBody(String[].class).returnResult().getResponseBody()))
                .toList())
        .containsExactly(trackedSubscription.getName());
  }

  @Test
  public void shouldNotAllowSubscriptionNameToContainDollarSign() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

    Stream.of("$name", "na$me", "name$")
        .forEach(
            name -> {
              // when
              WebTestClient.ResponseSpec response =
                  hermes.api().createSubscription(subscription(topic, name).build());

              // then
              response.expectStatus().isBadRequest();
            });
  }

  @Test
  public void shouldReturnHealthyStatusForAHealthySubscription() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

    // and
    prometheus.stubTopicMetrics(
        topicMetrics(topic.getName())
            .withDeliveryRate(100)
            .withRate(100)
            .withThroughput(0)
            .build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(subscription.getQualifiedName()).withRate(100).build());

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().getSubscriptionHealth(topic.getQualifiedName(), subscription.getName());

    // then
    assertThat(response.expectBody(SubscriptionHealth.class).returnResult().getResponseBody())
        .isEqualTo(SubscriptionHealth.HEALTHY);
  }

  @Test
  public void shouldReturnUnhealthyStatusWithAProblemForMalfunctioningSubscription() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

    // and
    prometheus.stubTopicMetrics(
        topicMetrics(topic.getName()).withDeliveryRate(100).withRate(50).withThroughput(0).build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(subscription.getQualifiedName()).withRate(50).with500Rate(11).build());

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().getSubscriptionHealth(topic.getQualifiedName(), subscription.getName());

    // then
    SubscriptionHealth subscriptionHealth =
        response.expectBody(SubscriptionHealth.class).returnResult().getResponseBody();
    assertThat(subscriptionHealth.getStatus()).isEqualTo(UNHEALTHY);
    assertThat(subscriptionHealth.getProblems())
        .containsOnly(malfunctioning(11, topic.getQualifiedName() + "$" + subscription.getName()));
  }

  @Test
  public void shouldReturnNoDataStatusWhenPrometheusRespondsWithAnError() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

    // and
    prometheus.stub500Error();

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().getSubscriptionHealth(topic.getQualifiedName(), subscription.getName());

    // then
    SubscriptionHealth subscriptionHealth =
        response.expectBody(SubscriptionHealth.class).returnResult().getResponseBody();
    assertThat(subscriptionHealth.getStatus()).isEqualTo(NO_DATA);
  }

  @Test
  public void shouldNotAllowSubscriptionWithBatchDeliveryAndAvroContentType() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        subscriptionWithRandomName(topic.getName())
            .withDeliveryType(DeliveryType.BATCH)
            .withContentType(ContentType.AVRO)
            .build();

    // when
    WebTestClient.ResponseSpec response = hermes.api().createSubscription(subscription);

    // then
    response.expectStatus().isBadRequest();
  }

  @Test
  public void shouldReturnConsumerGroupDescription() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint()).build());
    hermes.api().publishUntilStatus(topic.getQualifiedName(), MESSAGE.body(), 201);
    subscriber.waitUntilAnyMessageReceived();

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().getConsumerGroupsDescription(topic.getQualifiedName(), subscription.getName());

    // then
    response.expectStatus().isOk();
    List<ConsumerGroup> consumerGroups =
        response.expectBodyList(ConsumerGroup.class).returnResult().getResponseBody();
    assertThat(consumerGroups.size()).isGreaterThan(0);
    assertThat(consumerGroups)
        .flatExtracting("members")
        .flatExtracting("partitions")
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
            "partition", "topic", "offsetMetadata", "contentType")
        .containsOnlyOnce(new TopicPartition(-1, "any", 0, 1, "any", topic.getContentType()));
  }

  @Test
  public void shouldNotCreateSubscriptionNotOwnedByTheUser() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription")
            .withOwner(new OwnerId("Plaintext", "subscriptionOwner"))
            .build();
    TestSecurityProvider.setUserIsAdmin(false);

    // when
    WebTestClient.ResponseSpec response = hermes.api().createSubscription(subscription);

    // then
    response.expectStatus().isBadRequest();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains(
            "Provide an owner that includes you, you would not be able to manage this subscription later");
  }

  @Test
  public void shouldNotUpdateSubscriptionNotOwnedByTheUser() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());
    TestSecurityProvider.setUserIsAdmin(false);

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().updateSubscription(topic, subscription.getName(), PATCH_DATA);

    // then
    response.expectStatus().isBadRequest();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains(
            "Provide an owner that includes you, you would not be able to manage this subscription later");
  }

  @Test
  public void shouldAllowAdminToBypassSubscribingRestrictions() {
    // given
    Topic topic =
        hermes.initHelper().createTopic(topicWithRandomName().withSubscribingRestricted().build());
    Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription")
            .withEndpoint("http://localhost:8081/topics/test-topic")
            .build();
    TestSecurityProvider.setUserIsAdmin(true);

    // when
    WebTestClient.ResponseSpec response = hermes.api().createSubscription(subscription);

    // then
    response.expectStatus().isCreated();

    // when
    WebTestClient.ResponseSpec updateResponse =
        hermes.api().updateSubscription(topic, subscription.getName(), PATCH_DATA);

    // then
    updateResponse.expectStatus().isOk();
  }

  @Test
  public void shouldAllowTopicOwnerToBypassSubscribingRestrictions() {
    // given
    Topic topic =
        hermes.initHelper().createTopic(topicWithRandomName().withSubscribingRestricted().build());
    Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription")
            .withEndpoint("http://localhost:8081/topics/test-topic")
            .build();
    TestSecurityProvider.setUserIsAdmin(false);
    TestSecurityProvider.setUserAsOwner(topic.getOwner(), subscription.getOwner());

    // when
    WebTestClient.ResponseSpec response = hermes.api().createSubscription(subscription);

    // then
    response.expectStatus().isCreated();

    // when
    WebTestClient.ResponseSpec updateResponse =
        hermes.api().updateSubscription(topic, subscription.getName(), PATCH_DATA);

    // then
    updateResponse.expectStatus().isOk();
  }

  @Test
  public void shouldAllowPrivilegedSubscriberToBypassSubscribingRestrictions() {
    // given
    Topic topic =
        hermes.initHelper().createTopic(topicWithRandomName().withSubscribingRestricted().build());
    Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription")
            .withOwner(new OwnerId("Plaintext", "subscriberAllowedToAccessAnyTopic"))
            .withEndpoint("http://localhost:8081/topics/test-topic")
            .build();
    TestSecurityProvider.setUserIsAdmin(false);
    TestSecurityProvider.setUserAsOwner(subscription.getOwner());

    // when
    WebTestClient.ResponseSpec response = hermes.api().createSubscription(subscription);

    // then
    response.expectStatus().isCreated();

    // when
    WebTestClient.ResponseSpec updateResponse =
        hermes.api().updateSubscription(topic, subscription.getName(), PATCH_DATA);

    // then
    updateResponse.expectStatus().isOk();
  }

  @Test
  public void shouldRespectPrivilegedSubscriberProtocolsWhileCreatingSubscription() {
    // given
    Topic topic =
        hermes.initHelper().createTopic(topicWithRandomName().withSubscribingRestricted().build());
    Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription")
            .withOwner(new OwnerId("Plaintext", "subscriberAllowedToAccessAnyTopic"))
            .withEndpoint(
                "googlepubsub://pubsub.googleapis.com:443/projects/test-project/topics/test-topic")
            .build();
    TestSecurityProvider.setUserIsAdmin(false);
    TestSecurityProvider.setUserAsOwner(subscription.getOwner());

    // when
    WebTestClient.ResponseSpec response = hermes.api().createSubscription(subscription);

    // then
    response.expectStatus().isForbidden();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains(
            "Subscribing to this topic has been restricted. Contact the topic owner to create a new subscription.");
  }

  @Test
  public void shouldRespectPrivilegedSubscriberProtocolsWhileUpdatingEndpoint() {
    // given
    Topic topic =
        hermes.initHelper().createTopic(topicWithRandomName().withSubscribingRestricted().build());
    Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription")
            .withOwner(new OwnerId("Plaintext", "subscriberAllowedToAccessAnyTopic"))
            .withEndpoint("http://localhost:8081/topics/test-topic")
            .build();
    hermes.initHelper().createSubscription(subscription);
    TestSecurityProvider.setUserIsAdmin(false);
    TestSecurityProvider.setUserAsOwner(subscription.getOwner());

    // when
    WebTestClient.ResponseSpec response =
        hermes
            .api()
            .updateSubscription(
                topic,
                subscription.getName(),
                patchData()
                    .set(
                        "endpoint",
                        EndpointAddress.of(
                            "googlepubsub://pubsub.googleapis.com:443/projects/test-project/topics/test-topic"))
                    .build());

    // then
    response.expectStatus().isForbidden();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains(
            "Subscribing to this topic has been restricted. Contact the topic owner to modify the endpoint of this subscription.");
  }

  @Test
  public void shouldNotAllowUnprivilegedUserToCreateSubscriptionWhenSubscribingIsRestricted() {
    // given
    Topic topic =
        hermes.initHelper().createTopic(topicWithRandomName().withSubscribingRestricted().build());
    Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription")
            .withOwner(new OwnerId("Plaintext", "subscriptionOwner"))
            .build();
    TestSecurityProvider.setUserIsAdmin(false);
    TestSecurityProvider.setUserAsOwner(subscription.getOwner());

    // when
    WebTestClient.ResponseSpec response = hermes.api().createSubscription(subscription);

    // then
    response.expectStatus().isForbidden();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains(
            "Subscribing to this topic has been restricted. Contact the topic owner to create a new subscription.");
  }

  @Test
  public void shouldNotAllowUnprivilegedUserToUpdateEndpointWhenSubscribingIsRestricted() {
    // given
    Topic topic =
        hermes.initHelper().createTopic(topicWithRandomName().withSubscribingRestricted().build());
    Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription")
            .withOwner(new OwnerId("Plaintext", "subscriptionOwner"))
            .withEndpoint("http://localhost:8081/topics/test-topic")
            .build();
    hermes.initHelper().createSubscription(subscription);
    TestSecurityProvider.setUserIsAdmin(false);
    TestSecurityProvider.setUserAsOwner(subscription.getOwner());

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().updateSubscription(topic, subscription.getName(), PATCH_DATA);

    // then
    response.expectStatus().isForbidden();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains(
            "Subscribing to this topic has been restricted. Contact the topic owner to modify the endpoint of this subscription.");
  }

  @Test
  public void shouldSetInflightSizeToNullByDefault() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());
    TestSecurityProvider.setUserIsAdmin(false);

    // when
    Subscription response =
        hermes.api().getSubscription(topic.getQualifiedName(), subscription.getName());

    // then
    assertThat(response.getSerialSubscriptionPolicy().getInflightSize()).isNull();
  }

  @Test
  public void shouldReturnInflightSizeWhenSetToNonNullValue() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName())
                    .withSubscriptionPolicy(
                        SubscriptionPolicy.Builder.subscriptionPolicy()
                            .withInflightSize(42)
                            .build())
                    .build());
    TestSecurityProvider.setUserIsAdmin(false);

    // when
    Subscription response =
        hermes.api().getSubscription(topic.getQualifiedName(), subscription.getName());

    // then
    assertThat(response.getSerialSubscriptionPolicy().getInflightSize()).isEqualTo(42);
  }
}
