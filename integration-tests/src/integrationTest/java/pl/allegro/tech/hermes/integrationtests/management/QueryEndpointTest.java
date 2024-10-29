package pl.allegro.tech.hermes.integrationtests.management;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.integrationtests.prometheus.SubscriptionMetrics.subscriptionMetrics;
import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.groupWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.groupWithRandomNameContaining;
import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.groupWithRandomNameEndedWith;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomNameContaining;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomNameEndedWith;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionNameWithMetrics;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicNameWithMetrics;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.api.TrackingMode;
import pl.allegro.tech.hermes.integrationtests.prometheus.PrometheusExtension;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;

public class QueryEndpointTest {

  private static final String SCHEMA = AvroUserSchemaLoader.load().toString();

  @Order(0)
  @RegisterExtension
  public static final PrometheusExtension prometheus = new PrometheusExtension();

  @Order(1)
  @RegisterExtension
  public static final HermesExtension hermes = new HermesExtension().withPrometheus(prometheus);

  @Test
  public void shouldReturnAllGroupsWhenQueryIsEmpty() {
    // given
    createGroupWithRandomName();
    createGroupWithRandomName();

    // when
    List<Group> found =
        hermes
            .api()
            .queryGroups("{\"query\": {}}")
            .expectStatus()
            .isOk()
            .expectBodyList(Group.class)
            .returnResult()
            .getResponseBody();

    // then
    assertThat(found)
        .extracting(Group::getGroupName)
        .containsExactlyInAnyOrderElementsOf(hermes.api().getGroups());
  }

  @Test
  public void shouldReturnGroupsWithExactName() {
    // given
    Group group = createGroupWithRandomName();
    createGroupWithRandomName();

    // when
    List<Group> found =
        hermes
            .api()
            .queryGroups("{\"query\": {\"groupName\": \"" + group.getGroupName() + "\"}}")
            .expectStatus()
            .isOk()
            .expectBodyList(Group.class)
            .returnResult()
            .getResponseBody();

    // then
    assertThat(found).containsExactly(group);
  }

  @Test
  public void shouldReturnGroupsWithNameSuffix() {
    // given
    String suffix = "GroupSuffix";
    hermes.initHelper().createGroup(groupWithRandomNameEndedWith(suffix).build());
    createGroupWithRandomName();

    // when
    List<Group> found =
        hermes
            .api()
            .queryGroups("{\"query\": {\"groupName\": {\"like\": \".*" + suffix + "\"}}}")
            .expectStatus()
            .isOk()
            .expectBodyList(Group.class)
            .returnResult()
            .getResponseBody();

    // then
    List<String> groupsWithSuffix =
        hermes.api().getGroups().stream().filter(name -> name.endsWith(suffix)).toList();
    assertThat(found)
        .extracting(Group::getGroupName)
        .containsExactlyInAnyOrderElementsOf(groupsWithSuffix);
  }

  @Test
  public void shouldReturnGroupsWithNameContainingString() {
    // given
    String string = "SomeString";
    hermes.initHelper().createGroup(groupWithRandomNameContaining(string).build());
    createGroupWithRandomName();

    // when
    List<Group> found =
        hermes
            .api()
            .queryGroups("{\"query\": {\"groupName\": {\"like\": \".*" + string + ".*\"}}}")
            .expectStatus()
            .isOk()
            .expectBodyList(Group.class)
            .returnResult()
            .getResponseBody();

    // then
    List<String> groupsContainingString =
        hermes.api().getGroups().stream().filter(name -> name.contains(string)).toList();
    assertThat(found)
        .extracting(Group::getGroupName)
        .containsExactlyInAnyOrderElementsOf(groupsContainingString);
  }

  @Test
  public void shouldReturnAllTopicsWhenQueryIsEmpty() {
    // given
    createTopicWithRandomName();
    createTopicWithRandomName();

    // when
    List<Topic> found =
        hermes
            .api()
            .queryTopics("{\"query\": {}}")
            .expectStatus()
            .isOk()
            .expectBodyList(Topic.class)
            .returnResult()
            .getResponseBody();

    // then
    assertThat(found).containsExactlyInAnyOrderElementsOf(listAllTopics());
  }

  @Test
  public void shouldReturnTopicsWithExactName() {
    // given
    Topic topic = createTopicWithRandomName();
    createTopicWithRandomName();

    // when
    List<Topic> found =
        hermes
            .api()
            .queryTopics("{\"query\": {\"name\": \"" + topic.getQualifiedName() + "\"}}")
            .expectStatus()
            .isOk()
            .expectBodyList(Topic.class)
            .returnResult()
            .getResponseBody();

    // then
    assertThat(found).containsExactly(topic);
  }

  @Test
  public void shouldReturnTopicsWithNameSuffix() {
    // given
    String suffix = "TopicSuffix";
    hermes.initHelper().createTopic(topicWithRandomNameEndedWith(suffix).build());
    createTopicWithRandomName();

    // when
    List<Topic> found =
        hermes
            .api()
            .queryTopics("{\"query\": {\"name\": {\"like\": \".*" + suffix + "\"}}}")
            .expectStatus()
            .isOk()
            .expectBodyList(Topic.class)
            .returnResult()
            .getResponseBody();

    // then
    List<Topic> topicsWithSuffix =
        listAllTopics().stream()
            .filter(topic -> topic.getQualifiedName().endsWith(suffix))
            .toList();
    assertThat(found).containsExactlyInAnyOrderElementsOf(topicsWithSuffix);
  }

  @Test
  public void shouldReturnTopicsWithNameContainingString() {
    // given
    String string = "SomeString";
    hermes.initHelper().createTopic(topicWithRandomNameContaining(string).build());
    createTopicWithRandomName();

    // when
    List<Topic> found =
        hermes
            .api()
            .queryTopics("{\"query\": {\"name\": {\"like\": \".*" + string + ".*\"}}}")
            .expectStatus()
            .isOk()
            .expectBodyList(Topic.class)
            .returnResult()
            .getResponseBody();

    // then
    List<Topic> topicsContainingString =
        listAllTopics().stream()
            .filter(topic -> topic.getQualifiedName().contains(string))
            .toList();
    assertThat(found).containsExactlyInAnyOrderElementsOf(topicsContainingString);
  }

  @Test
  public void shouldReturnTopicsWithAllMatchingProperties() {
    // given
    Topic topic1 = topicWithRandomName().withContentType(AVRO).withTrackingEnabled(false).build();
    Topic topic2 = topicWithRandomName().withContentType(JSON).withTrackingEnabled(false).build();
    Topic topic3 = topicWithRandomName().withContentType(AVRO).withTrackingEnabled(true).build();
    hermes.initHelper().createTopicWithSchema(topicWithSchema(topic1, SCHEMA));
    hermes.initHelper().createTopicWithSchema(topicWithSchema(topic3, SCHEMA));
    hermes.initHelper().createTopic(topic2);

    // when
    List<Topic> found =
        hermes
            .api()
            .queryTopics(
                "{\"query\": {\"and\": [{\"trackingEnabled\": \"true\"}, {\"contentType\": \"AVRO\"}]}}")
            .expectStatus()
            .isOk()
            .expectBodyList(Topic.class)
            .returnResult()
            .getResponseBody();

    // then
    List<Topic> topicsWithAvroAndTracking =
        listAllTopics().stream()
            .filter(topic -> topic.getContentType() == AVRO && topic.isTrackingEnabled())
            .toList();
    assertThat(found).containsExactlyInAnyOrderElementsOf(topicsWithAvroAndTracking);
  }

  @Test
  public void shouldReturnTopicsWithAtLeastOneMatchingProperty() {
    // given
    Topic topic1 = topicWithRandomName().withContentType(AVRO).withTrackingEnabled(false).build();
    Topic topic2 = topicWithRandomName().withContentType(JSON).withTrackingEnabled(false).build();
    Topic topic3 = topicWithRandomName().withContentType(AVRO).withTrackingEnabled(true).build();
    hermes.initHelper().createTopicWithSchema(topicWithSchema(topic1, SCHEMA));
    hermes.initHelper().createTopicWithSchema(topicWithSchema(topic3, SCHEMA));
    hermes.initHelper().createTopic(topic2);

    // when
    List<Topic> found =
        hermes
            .api()
            .queryTopics(
                "{\"query\": {\"or\": [{\"trackingEnabled\": \"true\"}, {\"contentType\": \"AVRO\"}]}}")
            .expectStatus()
            .isOk()
            .expectBodyList(Topic.class)
            .returnResult()
            .getResponseBody();

    // then
    List<Topic> topicsWithAvroOrTracking =
        listAllTopics().stream()
            .filter(topic -> topic.getContentType() == AVRO || topic.isTrackingEnabled())
            .toList();
    assertThat(found).containsExactlyInAnyOrderElementsOf(topicsWithAvroOrTracking);
  }

  @Test
  public void shouldReturnTopicsWithExactOwnerId() {
    // given
    Topic topic =
        topicWithRandomName()
            .withContentType(JSON)
            .withTrackingEnabled(true)
            .withOwner(new OwnerId("Plaintext", "Team Alpha"))
            .build();
    hermes.initHelper().createTopic(topic);
    createTopicWithRandomName();

    // when
    List<Topic> found =
        hermes
            .api()
            .queryTopics("{\"query\": {\"owner.id\": \"Team Alpha\"}}")
            .expectStatus()
            .isOk()
            .expectBodyList(Topic.class)
            .returnResult()
            .getResponseBody();

    // then
    List<Topic> topicsOwnerId =
        listAllTopics().stream().filter(t -> t.getOwner().getId().equals("Team Alpha")).toList();
    assertThat(found).containsExactlyInAnyOrderElementsOf(topicsOwnerId);
  }

  @Test
  public void shouldReturnTopicsWithOwnerIdContainingString() {
    // given
    Topic topic =
        topicWithRandomName()
            .withContentType(JSON)
            .withTrackingEnabled(true)
            .withOwner(new OwnerId("Plaintext", "Team Alpha"))
            .build();
    hermes.initHelper().createTopic(topic);
    createTopicWithRandomName();

    // when
    List<Topic> found =
        hermes
            .api()
            .queryTopics("{\"query\": {\"owner.id\": {\"like\": \".*Alph.*\"}}}")
            .expectStatus()
            .isOk()
            .expectBodyList(Topic.class)
            .returnResult()
            .getResponseBody();

    // then
    List<Topic> topicsOwnerId =
        listAllTopics().stream().filter(t -> t.getOwner().getId().contains("Alph")).toList();
    assertThat(found).containsExactlyInAnyOrderElementsOf(topicsOwnerId);
  }

  private List<Topic> listAllTopics() {
    List<String> groups = hermes.api().getGroups();
    List<Topic> topics = new ArrayList<>();
    for (String groupName : groups) {
      String[] topicNames =
          hermes
              .api()
              .listTopics(groupName)
              .expectStatus()
              .isOk()
              .expectBody(String[].class)
              .returnResult()
              .getResponseBody();
      for (String topicName : topicNames) {
        TopicWithSchema topic =
            hermes
                .api()
                .getTopicResponse(topicName)
                .expectBody(TopicWithSchema.class)
                .returnResult()
                .getResponseBody();
        topics.add(topic.getTopic());
      }
    }
    return topics;
  }

  public static Stream<Arguments> subscriptionData() {
    return Stream.of(
        arguments("{\"query\": {}}", asList(1, 2, 3, 4)),
        arguments("{\"query\": {\"name\": \"subscription1\"}}", asList(1)),
        arguments("{\"query\": {\"name\": {\"like\": \".*cription1\"}}}", asList(1)),
        arguments("{\"query\": {\"name\": {\"like\": \"subscript.*\"}}}", asList(1, 2, 4)),
        arguments(
            "{\"query\": {\"name\": \"subscription1\", \"endpoint\": \"http://endpoint1\"}}",
            asList(1)),
        arguments(
            "{\"query\": {\"and\": [{\"name\": \"subscription1\"}, {\"endpoint\": \"http://endpoint1\"}]}}",
            asList(1)),
        arguments(
            "{\"query\": {\"or\": [{\"name\": \"subscription1\"}, {\"endpoint\": \"http://endpoint1\"}]}}",
            asList(1, 3)),
        arguments("{\"query\": {\"owner.id\": \"Team Alpha\"}}", asList(4)),
        arguments("{\"query\": {\"owner.id\": {\"like\": \".*Alph.*\"}}}", asList(4)),
        arguments("{\"query\": {\"endpoint\": \".*password.*\"}}", asList()));
  }

  @ParameterizedTest
  @MethodSource("subscriptionData")
  public void shouldQuerySubscription(String query, List<Integer> positions) {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

    Subscription subscription1 =
        hermes
            .initHelper()
            .createSubscription(
                enrichSubscription(
                    subscription(topic.getName(), "subscription1"), "http://endpoint1"));

    Subscription subscription2 =
        hermes
            .initHelper()
            .createSubscription(
                enrichSubscription(
                    subscription(topic.getName(), "subscription2"), "http://endpoint2"));

    Subscription subscription3 =
        hermes
            .initHelper()
            .createSubscription(
                enrichSubscription(
                    subscription(topic.getName(), "subTestScription3"),
                    "http://login:password@endpoint1"));

    Subscription subscription4 =
        hermes
            .initHelper()
            .createSubscription(
                enrichSubscription(
                    subscription(topic.getName(), "subscription4")
                        .withOwner(new OwnerId("Plaintext", "Team Alpha")),
                    "http://endpoint2"));

    List<Subscription> subscriptions =
        asList(subscription1, subscription2, subscription3.anonymize(), subscription4);

    // when
    List<Subscription> found =
        hermes
            .api()
            .querySubscriptions(query)
            .expectStatus()
            .isOk()
            .expectBodyList(Subscription.class)
            .returnResult()
            .getResponseBody();

    // then
    assertListMatches(subscriptions, found, positions);
  }

  @Test
  public void shouldSkipEntitiesNotContainingQueriedField() {
    // given
    hermes.initHelper().createGroup(groupWithRandomName().build());

    // when
    List<Group> found =
        hermes
            .api()
            .queryGroups("{\"query\": {\"missingField\": \"xxx\"}}")
            .expectStatus()
            .isOk()
            .expectBodyList(Group.class)
            .returnResult()
            .getResponseBody();

    // then
    Assertions.assertThat(found).isEmpty();
  }

  @Test
  public void shouldSkipEntitiesNotContainingQueriedNestedField() {
    // given
    hermes.initHelper().createGroup(groupWithRandomName().build());

    // when
    List<Group> found =
        hermes
            .api()
            .queryGroups("{\"query\": {\"missing.nested.field\": \"xxx\"}}")
            .expectStatus()
            .isOk()
            .expectBodyList(Group.class)
            .returnResult()
            .getResponseBody();

    // then
    Assertions.assertThat(found).isEmpty();
  }

  public static Stream<Arguments> topicsMetricsFilteringData() {
    return Stream.of(
        arguments(
            "testTopic1", "testTopic2", "{\"query\": {}}", asList("testTopic1", "testTopic2")),
        arguments(
            "testTopic3", "testTopic4", "{\"query\": {\"published\": {\"gt\": \"20\"}}}", asList()),
        arguments(
            "testTopic5",
            "testTopic6",
            "{\"query\": {\"published\": {\"gt\": \"3\"}}}",
            asList("testTopic6")),
        arguments(
            "testTopic7",
            "testTopic8",
            "{\"query\": {\"published\": {\"lt\": \"3\"}}}",
            asList("testTopic7")));
  }

  @ParameterizedTest
  @MethodSource("topicsMetricsFilteringData")
  public void shouldQueryTopicsMetrics(
      String topicName1, String topicName2, String query, List<String> topicNames) {
    // given
    Group group = groupWithRandomName().build();
    hermes.initHelper().createGroup(group);
    hermes
        .initHelper()
        .createTopic(
            topic(group.getGroupName(), topicName1)
                .withContentType(JSON)
                .withTrackingEnabled(false)
                .build());
    hermes
        .initHelper()
        .createTopic(
            topic(group.getGroupName(), topicName2)
                .withContentType(JSON)
                .withTrackingEnabled(false)
                .build());

    hermes.api().publishUntilSuccess(group.getGroupName() + "." + topicName1, "testMessage1");
    hermes.api().publishUntilSuccess(group.getGroupName() + "." + topicName2, "testMessage2");
    hermes.api().publishUntilSuccess(group.getGroupName() + "." + topicName2, "testMessage3");
    hermes.api().publishUntilSuccess(group.getGroupName() + "." + topicName2, "testMessage4");
    hermes.api().publishUntilSuccess(group.getGroupName() + "." + topicName2, "testMessage5");

    List<String> qualifiedNames =
        topicNames.stream()
            .map(topicName -> group.getGroupName() + "." + topicName)
            .collect(toList());

    waitAtMost(adjust(Duration.ofMinutes(1)))
        .untilAsserted(
            () -> {
              // when
              List<TopicNameWithMetrics> found =
                  hermes
                      .api()
                      .queryTopicMetrics(query)
                      .expectStatus()
                      .isOk()
                      .expectBodyList(TopicNameWithMetrics.class)
                      .returnResult()
                      .getResponseBody();

              // then
              assertTopicMetricsMatchesToNames(found, qualifiedNames);

              found.forEach(it -> Assertions.assertThat(it.getVolume()).isGreaterThanOrEqualTo(0));
            });
  }

  @Test
  public void shouldQuerySubscriptionsMetrics() {
    // given
    Topic topic1 = hermes.initHelper().createTopic(topicWithRandomName().build());
    Topic topic2 = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription1 =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic1.getName(), "http://endpoint1").build());
    Subscription subscription2 =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic2.getName(), "http://endpoint2").build());

    final String queryGetAllSubscriptionsMetrics = "{\"query\": {}}";
    final String queryGetSubscriptionsMetricsWithPositiveThroughput =
        "{\"query\": {\"throughput\": {\"gt\": 0}}}";
    final String queryGetSubscriptionsMetricsWithRateInRange =
        "{\"query\": {\"or\": [{\"rate\": {\"gt\": 10}}, {\"rate\": {\"lt\": 50}}]}}";
    final String queryGetSubscriptionsMetricsWithLagNegative =
        "{\"query\": {\"lag\": {\"lt\": 0}}}";
    final String queryGetSubscriptionsMetricsWithVolume = "{\"query\": {\"volume\": {\"gt\": -1}}}";
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(subscription1.getQualifiedName())
            .withRate(100)
            .withThroughput(0)
            .build());
    prometheus.stubSubscriptionMetrics(
        subscriptionMetrics(subscription2.getQualifiedName())
            .withRate(40)
            .withThroughput(10)
            .build());

    waitAtMost(adjust(Duration.ofMinutes(1)))
        .untilAsserted(
            () -> {
              // when
              final List<SubscriptionNameWithMetrics> allSubscriptions =
                  hermes
                      .api()
                      .querySubscriptionMetrics(queryGetAllSubscriptionsMetrics)
                      .expectStatus()
                      .isOk()
                      .expectBodyList(SubscriptionNameWithMetrics.class)
                      .returnResult()
                      .getResponseBody();
              final List<SubscriptionNameWithMetrics> subscriptionsWithPositiveThroughput =
                  hermes
                      .api()
                      .querySubscriptionMetrics(queryGetSubscriptionsMetricsWithPositiveThroughput)
                      .expectStatus()
                      .isOk()
                      .expectBodyList(SubscriptionNameWithMetrics.class)
                      .returnResult()
                      .getResponseBody();
              final List<SubscriptionNameWithMetrics> subscriptionsWithRateInRange =
                  hermes
                      .api()
                      .querySubscriptionMetrics(queryGetSubscriptionsMetricsWithRateInRange)
                      .expectStatus()
                      .isOk()
                      .expectBodyList(SubscriptionNameWithMetrics.class)
                      .returnResult()
                      .getResponseBody();
              final List<SubscriptionNameWithMetrics> subscriptionsWithNegativeLag =
                  hermes
                      .api()
                      .querySubscriptionMetrics(queryGetSubscriptionsMetricsWithLagNegative)
                      .expectStatus()
                      .isOk()
                      .expectBodyList(SubscriptionNameWithMetrics.class)
                      .returnResult()
                      .getResponseBody();
              final List<SubscriptionNameWithMetrics> subscriptionsWithVolume =
                  hermes
                      .api()
                      .querySubscriptionMetrics(queryGetSubscriptionsMetricsWithVolume)
                      .expectStatus()
                      .isOk()
                      .expectBodyList(SubscriptionNameWithMetrics.class)
                      .returnResult()
                      .getResponseBody();

              // then
              subscriptionsMatchesToNamesAndTheirTopicsNames(
                  allSubscriptions, subscription1, subscription2);
              subscriptionsMatchesToNamesAndTheirTopicsNames(
                  subscriptionsWithPositiveThroughput, subscription2);
              subscriptionsMatchesToNamesAndTheirTopicsNames(
                  subscriptionsWithRateInRange, subscription2);
              subscriptionsMatchesToNamesAndTheirTopicsNames(
                  subscriptionsWithNegativeLag, subscription1, subscription2);
              subscriptionsMatchesToNamesAndTheirTopicsNames(
                  subscriptionsWithVolume, subscription1, subscription2);
            });
  }

  @Test
  public void shouldHandleUnavailableSubscriptionsMetrics() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName(), "http://endpoint1").build());
    String queryGetAllSubscriptionsMetrics = "{\"query\": {}}";
    String queryGetSubscriptionsMetricsWithPositiveRate = "{\"query\": {\"rate\": {\"gt\": 0}}}";
    prometheus.stubDelay(Duration.ofMillis(3000));

    waitAtMost(adjust(Duration.ofMinutes(1)))
        .untilAsserted(
            () -> {
              // when
              List<SubscriptionNameWithMetrics> allSubscriptions =
                  hermes
                      .api()
                      .querySubscriptionMetrics(queryGetAllSubscriptionsMetrics)
                      .expectStatus()
                      .isOk()
                      .expectBodyList(SubscriptionNameWithMetrics.class)
                      .returnResult()
                      .getResponseBody();
              List<SubscriptionNameWithMetrics> subscriptionsWithPositiveRate =
                  hermes
                      .api()
                      .querySubscriptionMetrics(queryGetSubscriptionsMetricsWithPositiveRate)
                      .expectStatus()
                      .isOk()
                      .expectBodyList(SubscriptionNameWithMetrics.class)
                      .returnResult()
                      .getResponseBody();

              // then
              assertThatRateIsUnavailable(allSubscriptions, subscription);
              assertThatRateIsUnavailable(subscriptionsWithPositiveRate, subscription);
            });
  }

  private static void assertThatRateIsUnavailable(
      List<SubscriptionNameWithMetrics> allSubscriptions, Subscription... subscriptions) {
    subscriptionsMatchesToNamesAndTheirTopicsNames(allSubscriptions, subscriptions);
    for (SubscriptionNameWithMetrics metrics : allSubscriptions) {
      assertThat(metrics.getRate().asString()).isEqualTo("unavailable");
    }
  }

  private static void subscriptionsMatchesToNamesAndTheirTopicsNames(
      List<SubscriptionNameWithMetrics> found, Subscription... expectedSubscriptions) {
    assertThat(found).isNotNull();
    Map<String, String> foundSubscriptionsAndTheirTopicNames =
        found.stream()
            .collect(
                Collectors.toMap(
                    SubscriptionNameWithMetrics::getName,
                    SubscriptionNameWithMetrics::getTopicName));
    for (Subscription subscription : expectedSubscriptions) {
      assertThat(foundSubscriptionsAndTheirTopicNames).containsKeys(subscription.getName());
      assertThat(foundSubscriptionsAndTheirTopicNames.get(subscription.getName()))
          .isEqualTo(subscription.getQualifiedTopicName());
    }
  }

  private Subscription enrichSubscription(SubscriptionBuilder subscription, String endpoint) {
    return subscription
        .withTrackingMode(TrackingMode.TRACK_ALL)
        .withSubscriptionPolicy(subscriptionPolicy().applyDefaults().build())
        .withEndpoint(EndpointAddress.of(endpoint))
        .build();
  }

  private <T> void assertListMatches(List<T> elements, List<T> found, List<Integer> positions) {
    found.removeIf(o -> !elements.contains(o));
    List<T> expected = positions.stream().map(i -> elements.get(i - 1)).collect(toList());
    Assertions.assertThat(found).isSubsetOf(expected);
  }

  private void assertTopicMetricsMatchesToNames(
      List<TopicNameWithMetrics> found, List<String> expectedQualifiedNames) {
    List<String> foundQualifiedNames =
        found.stream().map(TopicNameWithMetrics::getName).collect(toList());

    Assertions.assertThat(foundQualifiedNames).containsAll(expectedQualifiedNames);
  }

  private Topic createTopicWithRandomName() {
    return hermes.initHelper().createTopic(topicWithRandomName().build());
  }

  private Group createGroupWithRandomName() {
    return hermes.initHelper().createGroup(groupWithRandomName().build());
  }
}
