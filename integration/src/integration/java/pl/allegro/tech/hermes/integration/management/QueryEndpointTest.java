package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionNameWithMetrics;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicNameWithMetrics;
import pl.allegro.tech.hermes.api.TrackingMode;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.helper.GraphiteEndpoint;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.integration.helper.GraphiteEndpoint.subscriptionMetricsStub;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class QueryEndpointTest extends IntegrationTest {

    private static final String SCHEMA = AvroUserSchemaLoader.load().toString();

    private RemoteServiceEndpoint remoteService;

    private GraphiteEndpoint graphiteEndpoint;

    @BeforeClass
    public void initialize() {
        remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
        graphiteEndpoint = new GraphiteEndpoint(SharedServices.services().graphiteHttpMock());
    }

    @AfterClass
    public void tearDown() {
        remoteService.reset();
    }

    @DataProvider(name = "groupData")
    public static Object[][] groupData() {
        return new Object[][]{
                {"{\"query\": {}}", asList(1, 2, 3, 4)},
                {"{\"query\": {\"groupName\": \"testGroup1\"}}", asList(1)},
                {"{\"query\": {\"groupName\": {\"like\": \".*Group2\"}}}", asList(3)},
                {"{\"query\": {\"groupName\": {\"like\": \".*Group.*\"}}}", asList(1, 3, 4)},
        };
    }

    @Test(dataProvider = "groupData")
    public void shouldQueryGroup(String query, List<Integer> positions) {
        // given
        Group group1 = new Group("testGroup1", "Support3");
        Group group2 = new Group("testNot1", "Support3");
        Group group3 = new Group("testGroup2", "Support4");
        Group group4 = new Group("testGroup3", "Support2");

        List<Group> groups = asList(group1, group2, group3, group4);

        groups.forEach(g -> management.group().create(g));

        // when
        List<Group> found = management.query().queryGroups(query);

        // then
        assertListMatches(groups, found, positions);
    }

    @DataProvider(name = "topicData")
    public static Object[][] topicData() {
        return new Object[][]{
                {"{\"query\": {}}", asList(1, 2, 3, 4)},
                {"{\"query\": {\"name\": \"testGroup1.testTopic1\"}}", asList(1)},
                {"{\"query\": {\"name\": {\"like\": \".*testTopic1\"}}}", asList(1)},
                {"{\"query\": {\"name\": {\"like\": \".*testTopic.*\"}}}", asList(1, 2, 3)},
                {"{\"query\": {\"trackingEnabled\": \"true\", \"contentType\": \"AVRO\"}}", asList(3)},
                {"{\"query\": {\"and\"" +
                        ": [{\"trackingEnabled\": \"true\"}, {\"contentType\": \"AVRO\"}]}}", asList(3)},
                {"{\"query\": {\"or\": [{\"trackingEnabled\": \"true\"}, {\"contentType\": \"AVRO\"}]}}", asList(1, 3, 4)},
                {"{\"query\": {\"owner.id\": \"Team Alpha\"}}", asList(4)},
                {"{\"query\": {\"owner.id\": {\"like\": \".*Alph.*\"}}}", asList(4)},
        };
    }

    @Test(dataProvider = "topicData")
    public void shouldQueryTopic(String query, List<Integer> positions) {
        // given
        Topic topic1 = topic("testGroup1", "testTopic1").withContentType(AVRO).withTrackingEnabled(false).build();
        operations.buildTopicWithSchema(topicWithSchema(topic1, SCHEMA));
        Topic topic2 = operations.buildTopic(topic("testGroup1", "testTopic2").withContentType(JSON).withTrackingEnabled(false).build()).getTopic();
        Topic topic3 = topic("testGroup1", "testTopic3").withContentType(AVRO).withTrackingEnabled(true).build();
        operations.buildTopicWithSchema(topicWithSchema(topic3, SCHEMA));
        Topic topic4 = operations.buildTopic(topic("testGroup2", "testOtherTopic").withContentType(JSON).withTrackingEnabled(true)
                .withOwner(new OwnerId("Plaintext", "Team Alpha")).build()
        ).getTopic();

        List<Topic> topics = asList(topic1, topic2, topic3, topic4);

        // when
        List<Topic> found = management.query().queryTopics(query);

        // then
        assertListMatches(topics, found, positions);
    }

    @DataProvider(name = "subscriptionData")
    public static Object[][] subscriptionData() {
        return new Object[][]{
                {"{\"query\": {}}", asList(1, 2, 3, 4)},
                {"{\"query\": {\"name\": \"subscription1\"}}", asList(1)},
                {"{\"query\": {\"name\": {\"like\": \".*cription1\"}}}", asList(1)},
                {"{\"query\": {\"name\": {\"like\": \"subscript.*\"}}}", asList(1, 2, 4)},
                {"{\"query\": {\"name\": \"subscription1\", \"endpoint\": \"http://endpoint1\"}}", asList(1)},
                {"{\"query\": {\"and\": [{\"name\": \"subscription1\"}, {\"endpoint\": \"http://endpoint1\"}]}}", asList(1)},
                {"{\"query\": {\"or\": [{\"name\": \"subscription1\"}, {\"endpoint\": \"http://endpoint1\"}]}}", asList(1, 3)},
                {"{\"query\": {\"owner.id\": \"Team Alpha\"}}", asList(4)},
                {"{\"query\": {\"owner.id\": {\"like\": \".*Alph.*\"}}}", asList(4)},
        };
    }

    @Test(dataProvider = "subscriptionData")
    public void shouldQuerySubscription(String query, List<Integer> positions) {
        // given
        Topic topic = operations.buildTopic(randomTopic("NotWise", "NotUsed").build());

        Subscription subscription1 = operations.createSubscription(topic, enrichSubscription(subscription(topic.getName(), "subscription1"), "http://endpoint1"));
        Subscription subscription2 = operations.createSubscription(topic, enrichSubscription(subscription(topic.getName(), "subscription2"), "http://endpoint2"));
        Subscription subscription3 = operations.createSubscription(topic, enrichSubscription(subscription(topic.getName(), "subTestScription3"), "http://endpoint1"));
        Subscription subscription4 = operations.createSubscription(topic, enrichSubscription(subscription(topic.getName(), "subscription4")
                .withOwner(new OwnerId("Plaintext", "Team Alpha")), "http://endpoint2")
        );

        List<Subscription> subscriptions = asList(subscription1, subscription2, subscription3, subscription4);

        // when
        List<Subscription> found = management.query().querySubscriptions(query);

        // then
        assertListMatches(subscriptions, found, positions);
    }

    @Test
    public void shouldSkipEntitiesNotContainingQueriedField() {
        // given
        management.group().create(new Group("group", "owner"));

        // when
        List<Group> found = management.query().queryGroups("{\"query\": {\"missingField\": \"xxx\"}}");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    public void shouldSkipEntitiesNotContainingQueriedNestedField() {
        // given
        management.group().create(new Group("group", "owner"));

        // when
        List<Group> found = management.query().queryGroups("{\"query\": {\"missing.nested.field\": \"xxx\"}}");

        // then
        assertThat(found).isEmpty();
    }

    @DataProvider(name = "topicsMetricsFilteringData")
    public static Object[][] topicsMetricsFilteringData() {
        return new Object[][]{
                {"testTopic1", "testTopic2", "{\"query\": {}}", asList("testGroup.testTopic1", "testGroup.testTopic2")},
                {"testTopic3", "testTopic4", "{\"query\": {\"published\": {\"gt\": \"5\"}}}", asList()},
                {"testTopic5", "testTopic6", "{\"query\": {\"published\": {\"gt\": \"2\"}}}", asList("testGroup.testTopic6")},
                {"testTopic7", "testTopic8", "{\"query\": {\"published\": {\"lt\": \"2\"}}}", asList("testGroup.testTopic7")},
        };
    }

    @Test(dataProvider = "topicsMetricsFilteringData")
    public void shouldQueryTopicsMetrics(String topicName1, String topicName2, String query, List<String> qualifiedNames) {
        // given
        operations.buildTopic(topic("testGroup", topicName1).withContentType(JSON).withTrackingEnabled(false).build());
        operations.buildTopic(topic("testGroup", topicName2).withContentType(JSON).withTrackingEnabled(false).build());

        publisher.publish("testGroup." + topicName1, "testMessage1");
        publisher.publish("testGroup." + topicName2, "testMessage2");
        publisher.publish("testGroup." + topicName2, "testMessage3");
        publisher.publish("testGroup." + topicName2, "testMessage4");

        wait.until(() -> {
            // when
            List<TopicNameWithMetrics> found = management.query().queryTopicsMetrics(query);

            // then
            assertTopicMetricsMatchesToNames(found, qualifiedNames);

            found.forEach(it -> assertThat(it.getVolume()).isGreaterThanOrEqualTo(0));
        });

    }

    @Test
    public void shouldQuerySubscriptionsMetrics() {
        // given
        Topic topic1 = operations.buildTopic("subscriptionsMetricsTestGroup1", "topic");
        Topic topic2 = operations.buildTopic("subscriptionsMetricsTestGroup2", "topic");

        Subscription subscription1 = operations.createSubscription(topic1, "subscription1", HTTP_ENDPOINT_URL);
        Subscription subscription2 = operations.createSubscription(topic2, "subscription2", HTTP_ENDPOINT_URL);

        String queryGetAllSubscriptionsMetrics = "{\"query\": {}}";
        String queryGetSubscriptionsMetricsWithPositiveThroughput = "{\"query\": {\"throughput\": {\"gt\": 0}}}";
        String queryGetSubscriptionsMetricsWithRateInRange = "{\"query\": {\"or\": [{\"rate\": {\"gt\": 10}}, {\"rate\": {\"lt\": 50}}]}}";
        String queryGetSubscriptionsMetricsWithLagNegative = "{\"query\": {\"lag\": {\"lt\": 0}}}";
        String queryGetSubscriptionsMetricsWithVolume = "{\"query\": {\"volume\": {\"gt\": -1}}}";

        graphiteEndpoint.returnMetric(subscriptionMetricsStub("subscriptionsMetricsTestGroup1.topic.subscription1")
                .withRate(100)
                .withThroughput(0)
                .build()
        );
        graphiteEndpoint.returnMetric(subscriptionMetricsStub("subscriptionsMetricsTestGroup2.topic.subscription2")
                .withRate(40)
                .withThroughput(10)
                .build()
        );

        wait.until(() -> {
            // when
            List<SubscriptionNameWithMetrics> allSubscriptions = management.query()
                    .querySubscriptionsMetrics(queryGetAllSubscriptionsMetrics);
            List<SubscriptionNameWithMetrics> subscriptionsWithPositiveThroughput = management.query()
                    .querySubscriptionsMetrics(queryGetSubscriptionsMetricsWithPositiveThroughput);
            List<SubscriptionNameWithMetrics> subscriptionsWithRateInRange = management.query()
                    .querySubscriptionsMetrics(queryGetSubscriptionsMetricsWithRateInRange);
            List<SubscriptionNameWithMetrics> subscriptionsWithNegativeLag = management.query()
                    .querySubscriptionsMetrics(queryGetSubscriptionsMetricsWithLagNegative);
            List<SubscriptionNameWithMetrics> subscriptionsWithVolume = management.query()
                    .querySubscriptionsMetrics(queryGetSubscriptionsMetricsWithVolume);

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
        Topic topic = operations.buildTopic("unavailableMetricsGroup", "topic");
        Subscription subscription = operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        String queryGetAllSubscriptionsMetrics = "{\"query\": {}}";
        String queryGetSubscriptionsMetricsWithPositiveRate = "{\"query\": {\"rate\": {\"gt\": 0}}}";

        int graphiteResponseDelay = 10 * 60 * 1000;
        graphiteEndpoint.returnMetricWithDelay(subscriptionMetricsStub("unavailableMetricsGroup.topic.subscription")
                .withRate(100)
                .build(),
                graphiteResponseDelay
        );

        wait.until(() -> {
            // when
            List<SubscriptionNameWithMetrics> allSubscriptions = management.query()
                    .querySubscriptionsMetrics(queryGetAllSubscriptionsMetrics);
            List<SubscriptionNameWithMetrics> subscriptionsWithPositiveRate = management.query()
                    .querySubscriptionsMetrics(queryGetSubscriptionsMetricsWithPositiveRate);

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
        assertThat(found).containsOnlyElementsOf(expected);
    }

    private void assertTopicMetricsMatchesToNames(List<TopicNameWithMetrics> found, List<String> expectedQualifiedNames) {
        List<String> foundQualifiedNames = found.stream()
                .map(TopicNameWithMetrics::getQualifiedName)
                .collect(Collectors.toList());

        assertThat(foundQualifiedNames).containsAll(expectedQualifiedNames);
    }

    private static void subscriptionsMatchesToNamesAndTheirTopicsNames(List<SubscriptionNameWithMetrics> found,
                                                                       Subscription ... expectedSubscriptions) {

        Map<String, String> foundSubscriptionsAndTheirTopicNames = found.stream()
                .collect(Collectors.toMap(SubscriptionNameWithMetrics::getName, SubscriptionNameWithMetrics::getTopicQualifiedName));

        for (Subscription subscription: expectedSubscriptions) {
            assertThat(foundSubscriptionsAndTheirTopicNames).containsKeys(subscription.getName());
            assertThat(foundSubscriptionsAndTheirTopicNames.get(subscription.getName())).isEqualTo(subscription.getQualifiedTopicName());
        }
    }
}
