package pl.allegro.tech.hermes.integrationtests.management;

import com.jayway.awaitility.Duration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
import pl.allegro.tech.hermes.api.TrackingMode;
import pl.allegro.tech.hermes.integrationtests.prometheus.PrometheusExtension;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.integrationtests.prometheus.SubscriptionMetrics.subscriptionMetrics;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class QueryEndpointTest {

    private static final String SCHEMA = AvroUserSchemaLoader.load().toString();

    @Order(0)
    @RegisterExtension
    public static final PrometheusExtension prometheus = new PrometheusExtension();

    @Order(1)
    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension()
            .withPrometheus(prometheus);

    @BeforeEach
    void cleanup() {
        hermes.clearManagementData();
    }

    public static Stream<Arguments> groupData() {
        return Stream.of(
                arguments("{\"query\": {}}", asList(1, 2, 3, 4)),
                arguments("{\"query\": {\"groupName\": \"testGroup1\"}}", asList(1)),
                arguments("{\"query\": {\"groupName\": {\"like\": \".*Group2\"}}}", asList(3)),
                arguments("{\"query\": {\"groupName\": {\"like\": \".*Group.*\"}}}", asList(1, 3, 4)));
    }

    @ParameterizedTest
    @MethodSource("groupData")
    public void shouldQueryGroup(String query, List<Integer> positions) {
        // given
        List<Group> groups = List.of(
                new Group("testGroup1"),
                new Group("testNot1"),
                new Group("testGroup2"),
                new Group("testGroup3")
        );

        groups.forEach(g -> hermes.initHelper().createGroup(g));

        // when
        List<Group> found = hermes.api().queryGroups(query)
                .expectStatus().isOk()
                .expectBodyList(Group.class).returnResult().getResponseBody();

        // then
        assertListMatches(groups, found, positions);
    }

    public static Stream<Arguments> topicData() {
        return Stream.of(
                arguments("{\"query\": {}}", asList(1, 2, 3, 4)),
                arguments("{\"query\": {\"name\": \"testGroup1.testTopic1\"}}", asList(1)),
                arguments("{\"query\": {\"name\": {\"like\": \".*testTopic1\"}}}", asList(1)),
                arguments("{\"query\": {\"name\": {\"like\": \".*testTopic.*\"}}}", asList(1, 2, 3)),
                arguments("{\"query\": {\"trackingEnabled\": \"true\", \"contentType\": \"AVRO\"}}", asList(3)),
                arguments("{\"query\": {\"and\": [{\"trackingEnabled\": \"true\"}, {\"contentType\": \"AVRO\"}]}}", asList(3)),
                arguments("{\"query\": {\"or\": [{\"trackingEnabled\": \"true\"}, {\"contentType\": \"AVRO\"}]}}", asList(1, 3, 4)),
                arguments("{\"query\": {\"owner.id\": \"Team Alpha\"}}", asList(4)),
                arguments("{\"query\": {\"owner.id\": {\"like\": \".*Alph.*\"}}}", asList(4)));
    }

    @ParameterizedTest
    @MethodSource("topicData")
    public void shouldQueryTopic(String query, List<Integer> positions) {
        // given
        Topic topic1 = topic("testGroup1", "testTopic1").withContentType(AVRO).withTrackingEnabled(false).build();
        Topic topic2 = topic("testGroup1", "testTopic2").withContentType(JSON).withTrackingEnabled(false).build();
        Topic topic3 = topic("testGroup1", "testTopic3").withContentType(AVRO).withTrackingEnabled(true).build();
        Topic topic4 = topic("testGroup2", "testOtherTopic").withContentType(JSON).withTrackingEnabled(true)
                .withOwner(new OwnerId("Plaintext", "Team Alpha")).build();

        hermes.initHelper().createTopicWithSchema(topicWithSchema(topic1, SCHEMA));
        hermes.initHelper().createTopicWithSchema(topicWithSchema(topic3, SCHEMA));
        hermes.initHelper().createTopic(topic2);
        hermes.initHelper().createTopic(topic4);

        List<Topic> topics = asList(topic1, topic2, topic3, topic4);

        // when
        List<Topic> found = hermes.api().queryTopics(query)
                .expectStatus().isOk()
                .expectBodyList(Topic.class).returnResult().getResponseBody();

        // then
        assertListMatches(topics, found, positions);
    }


    public static Stream<Arguments> subscriptionData() {
        return Stream.of(
                arguments("{\"query\": {}}", asList(1, 2, 3, 4)),
                arguments("{\"query\": {\"name\": \"subscription1\"}}", asList(1)),
                arguments("{\"query\": {\"name\": {\"like\": \".*cription1\"}}}", asList(1)),
                arguments("{\"query\": {\"name\": {\"like\": \"subscript.*\"}}}", asList(1, 2, 4)),
                arguments("{\"query\": {\"name\": \"subscription1\", \"endpoint\": \"http://endpoint1\"}}", asList(1)),
                arguments("{\"query\": {\"and\": [{\"name\": \"subscription1\"}, {\"endpoint\": \"http://endpoint1\"}]}}", asList(1)),
                arguments("{\"query\": {\"or\": [{\"name\": \"subscription1\"}, {\"endpoint\": \"http://endpoint1\"}]}}", asList(1, 3)),
                arguments("{\"query\": {\"owner.id\": \"Team Alpha\"}}", asList(4)),
                arguments("{\"query\": {\"owner.id\": {\"like\": \".*Alph.*\"}}}", asList(4)),
                arguments("{\"query\": {\"endpoint\": \".*password.*\"}}", asList())
        );
    }

    @ParameterizedTest
    @MethodSource("subscriptionData")
    public void shouldQuerySubscription(String query, List<Integer> positions) {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        Subscription subscription1 = hermes.initHelper().createSubscription(
                enrichSubscription(subscription(topic.getName(), "subscription1"), "http://endpoint1")
        );

        Subscription subscription2 = hermes.initHelper().createSubscription(
                enrichSubscription(subscription(topic.getName(), "subscription2"), "http://endpoint2")
        );

        Subscription subscription3 = hermes.initHelper().createSubscription(
                enrichSubscription(subscription(topic.getName(), "subTestScription3"), "http://login:password@endpoint1")
        );

        Subscription subscription4 = hermes.initHelper().createSubscription(enrichSubscription(subscription(topic.getName(), "subscription4")
                .withOwner(new OwnerId("Plaintext", "Team Alpha")), "http://endpoint2")
        );

        List<Subscription> subscriptions = asList(subscription1, subscription2, subscription3.anonymize(), subscription4);

        // when
        List<Subscription> found = hermes.api().querySubscriptions(query)
                .expectStatus().isOk()
                .expectBodyList(Subscription.class).returnResult().getResponseBody();

        // then
        assertListMatches(subscriptions, found, positions);
    }

    @Test
    public void shouldSkipEntitiesNotContainingQueriedField() {
        // given
        hermes.initHelper().createGroup(new Group("group"));

        // when
        List<Group> found = hermes.api().queryGroups("{\"query\": {\"missingField\": \"xxx\"}}")
                .expectStatus().isOk()
                .expectBodyList(Group.class).returnResult().getResponseBody();

        // then
        Assertions.assertThat(found).isEmpty();
    }

    @Test
    public void shouldSkipEntitiesNotContainingQueriedNestedField() {
        // given
        hermes.initHelper().createGroup(new Group("group"));

        // when
        List<Group> found = hermes.api().queryGroups("{\"query\": {\"missing.nested.field\": \"xxx\"}}")
                .expectStatus().isOk()
                .expectBodyList(Group.class).returnResult().getResponseBody();

        // then
        Assertions.assertThat(found).isEmpty();
    }

    public static Stream<Arguments> topicsMetricsFilteringData() {
        return Stream.of(
                arguments("testTopic1", "testTopic2", "{\"query\": {}}", asList("testGroup.testTopic1", "testGroup.testTopic2")),
                arguments("testTopic3", "testTopic4", "{\"query\": {\"published\": {\"gt\": \"5\"}}}", asList()),
                arguments("testTopic5", "testTopic6", "{\"query\": {\"published\": {\"gt\": \"2\"}}}", asList("testGroup.testTopic6")),
                arguments("testTopic7", "testTopic8", "{\"query\": {\"published\": {\"lt\": \"2\"}}}", asList("testGroup.testTopic7")));
    }

    @ParameterizedTest
    @MethodSource("topicsMetricsFilteringData")
    public void shouldQueryTopicsMetrics(String topicName1, String topicName2, String query, List<String> qualifiedNames) {
        // given
        hermes.initHelper().createTopic(topic("testGroup", topicName1).withContentType(JSON).withTrackingEnabled(false).build());
        hermes.initHelper().createTopic(topic("testGroup", topicName2).withContentType(JSON).withTrackingEnabled(false).build());

        hermes.api().publish("testGroup." + topicName1, "testMessage1");
        hermes.api().publish("testGroup." + topicName2, "testMessage2");
        hermes.api().publish("testGroup." + topicName2, "testMessage3");
        hermes.api().publish("testGroup." + topicName2, "testMessage4");

        waitAtMost(adjust(Duration.ONE_MINUTE)).until(() -> {
                    // when
                    List<TopicNameWithMetrics> found = hermes.api().queryTopicMetrics(query)
                            .expectStatus().isOk()
                            .expectBodyList(TopicNameWithMetrics.class).returnResult().getResponseBody();

                    // then
                    assertTopicMetricsMatchesToNames(found, qualifiedNames);

                    found.forEach(it -> Assertions.assertThat(it.getVolume()).isGreaterThanOrEqualTo(0));
                }
        );
    }

    @Test
    public void shouldQuerySubscriptionsMetrics() {
        // given
        Topic topic1 = hermes.initHelper().createTopic(topicWithRandomName().build());
        Topic topic2 = hermes.initHelper().createTopic(topicWithRandomName().build());
        Subscription subscription1 = hermes.initHelper().createSubscription(
                subscriptionWithRandomName(topic1.getName(), "http://endpoint1").build()
        );
        Subscription subscription2 = hermes.initHelper().createSubscription(
                subscriptionWithRandomName(topic2.getName(), "http://endpoint2").build()
        );

        String queryGetAllSubscriptionsMetrics = "{\"query\": {}}";
        String queryGetSubscriptionsMetricsWithPositiveThroughput = "{\"query\": {\"throughput\": {\"gt\": 0}}}";
        String queryGetSubscriptionsMetricsWithRateInRange = "{\"query\": {\"or\": [{\"rate\": {\"gt\": 10}}, {\"rate\": {\"lt\": 50}}]}}";
        String queryGetSubscriptionsMetricsWithLagNegative = "{\"query\": {\"lag\": {\"lt\": 0}}}";
        String queryGetSubscriptionsMetricsWithVolume = "{\"query\": {\"volume\": {\"gt\": -1}}}";
        prometheus.stubSubscriptionMetrics(
                subscriptionMetrics(subscription1.getQualifiedName())
                        .withRate(100)
                        .withThroughput(0)
                        .build()
        );
        prometheus.stubSubscriptionMetrics(
                subscriptionMetrics(subscription2.getQualifiedName())
                        .withRate(40)
                        .withThroughput(10)
                        .build()
        );

        waitAtMost(adjust(Duration.ONE_MINUTE)).until(() -> {
            // when
            List<SubscriptionNameWithMetrics> allSubscriptions = hermes.api()
                    .querySubscriptionMetrics(queryGetAllSubscriptionsMetrics)
                    .expectStatus().isOk()
                    .expectBodyList(SubscriptionNameWithMetrics.class).returnResult().getResponseBody();
            List<SubscriptionNameWithMetrics> subscriptionsWithPositiveThroughput = hermes.api()
                    .querySubscriptionMetrics(queryGetSubscriptionsMetricsWithPositiveThroughput)
                    .expectStatus().isOk()
                    .expectBodyList(SubscriptionNameWithMetrics.class).returnResult().getResponseBody();
            List<SubscriptionNameWithMetrics> subscriptionsWithRateInRange = hermes.api()
                    .querySubscriptionMetrics(queryGetSubscriptionsMetricsWithRateInRange)
                    .expectStatus().isOk()
                    .expectBodyList(SubscriptionNameWithMetrics.class).returnResult().getResponseBody();
            List<SubscriptionNameWithMetrics> subscriptionsWithNegativeLag = hermes.api()
                    .querySubscriptionMetrics(queryGetSubscriptionsMetricsWithLagNegative)
                    .expectStatus().isOk()
                    .expectBodyList(SubscriptionNameWithMetrics.class).returnResult().getResponseBody();
            List<SubscriptionNameWithMetrics> subscriptionsWithVolume = hermes.api()
                    .querySubscriptionMetrics(queryGetSubscriptionsMetricsWithVolume)
                    .expectStatus().isOk()
                    .expectBodyList(SubscriptionNameWithMetrics.class).returnResult().getResponseBody();

            // then
            subscriptionsMatchesToNamesAndTheirTopicsNames(allSubscriptions, subscription1, subscription2);
            subscriptionsMatchesToNamesAndTheirTopicsNames(subscriptionsWithPositiveThroughput, subscription2);
            subscriptionsMatchesToNamesAndTheirTopicsNames(subscriptionsWithRateInRange, subscription2);
            subscriptionsMatchesToNamesAndTheirTopicsNames(subscriptionsWithNegativeLag, subscription1, subscription2);
            subscriptionsMatchesToNamesAndTheirTopicsNames(subscriptionsWithVolume, subscription1, subscription2);
        });
    }

    @Test
    public void shouldHandleUnavailableSubscriptionsMetrics() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        Subscription subscription = hermes.initHelper().createSubscription(
                subscriptionWithRandomName(topic.getName(), "http://endpoint1").build()
        );
        String queryGetAllSubscriptionsMetrics = "{\"query\": {}}";
        String queryGetSubscriptionsMetricsWithPositiveRate = "{\"query\": {\"rate\": {\"gt\": 0}}}";
        prometheus.stubDelay(ofMinutes(10));

        waitAtMost(adjust(Duration.ONE_MINUTE)).until(() -> {
            // when
            List<SubscriptionNameWithMetrics> allSubscriptions = hermes.api()
                    .querySubscriptionMetrics(queryGetAllSubscriptionsMetrics)
                    .expectStatus().isOk()
                    .expectBodyList(SubscriptionNameWithMetrics.class).returnResult().getResponseBody();
            List<SubscriptionNameWithMetrics> subscriptionsWithPositiveRate = hermes.api()
                    .querySubscriptionMetrics(queryGetSubscriptionsMetricsWithPositiveRate)
                    .expectStatus().isOk()
                    .expectBodyList(SubscriptionNameWithMetrics.class).returnResult().getResponseBody();

            // then
            assertThatRateIsUnavailable(allSubscriptions, subscription);
            assertThatRateIsUnavailable(subscriptionsWithPositiveRate, subscription);
        });
    }

    private static void assertThatRateIsUnavailable(List<SubscriptionNameWithMetrics> allSubscriptions, Subscription ... subscriptions) {
        subscriptionsMatchesToNamesAndTheirTopicsNames(allSubscriptions, subscriptions);
        for (SubscriptionNameWithMetrics metrics : allSubscriptions) {
            assertThat(metrics.getRate().asString()).isEqualTo("unavailable");
        }
    }

    private static void subscriptionsMatchesToNamesAndTheirTopicsNames(List<SubscriptionNameWithMetrics> found,
                                                                       Subscription ... expectedSubscriptions) {
        assertThat(found).isNotNull();
        Map<String, String> foundSubscriptionsAndTheirTopicNames = found.stream()
                .collect(Collectors.toMap(SubscriptionNameWithMetrics::getName, SubscriptionNameWithMetrics::getTopicName));
        for (Subscription subscription : expectedSubscriptions) {
            assertThat(foundSubscriptionsAndTheirTopicNames).containsKeys(subscription.getName());
            assertThat(foundSubscriptionsAndTheirTopicNames.get(subscription.getName())).isEqualTo(subscription.getQualifiedTopicName());
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
        List<T> expected = positions.stream().map(i -> elements.get(i - 1)).collect(Collectors.toList());
        Assertions.assertThat(found).isSubsetOf(expected);
    }

    private void assertTopicMetricsMatchesToNames(List<TopicNameWithMetrics> found, List<String> expectedQualifiedNames) {
        List<String> foundQualifiedNames = found.stream()
                .map(TopicNameWithMetrics::getName)
                .collect(Collectors.toList());

        Assertions.assertThat(foundQualifiedNames).containsAll(expectedQualifiedNames);
    }
}
