package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class QueryEndpointTest extends IntegrationTest {

    @DataProvider(name = "groupData")
    public static Object[][] groupData() {
        return new Object[][] {
                {"{\"query\": {}}", asList(1, 2, 3, 4)},
                {"{\"query\": {\"groupName\": \"testGroup1\"}}", asList(1)},
                {"{\"query\": {\"groupName\": {\"like\": \".*Group2\"}}}", asList(3)},
                {"{\"query\": {\"groupName\": {\"like\": \".*Group.*\"}}}", asList(1, 3, 4)},
                {"{\"query\": {\"technicalOwner\": \"Owner2\", \"supportTeam\": \"Support3\"}}", asList(2)},
                {"{\"query\": {\"and\": [{\"technicalOwner\": \"Owner2\"}, {\"supportTeam\": \"Support3\"}]}}", asList(2)},
                {"{\"query\": {\"or\": [{\"technicalOwner\": \"Owner2\"}, {\"supportTeam\": \"Support3\"}]}}", asList(1, 2, 3)},
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
        return new Object[][] {
                {"{\"query\": {}}", asList(1, 2, 3, 4)},
                {"{\"query\": {\"name\": \"testGroup1.testTopic1\"}}", asList(1)},
                {"{\"query\": {\"name\": {\"like\": \".*testTopic1\"}}}", asList(1)},
                {"{\"query\": {\"name\": {\"like\": \".*testTopic.*\"}}}", asList(1, 2, 3)},
                {"{\"query\": {\"trackingEnabled\": \"true\", \"contentType\": \"AVRO\"}}", asList(3)},
                {"{\"query\": {\"and\": [{\"trackingEnabled\": \"true\"}, {\"contentType\": \"AVRO\"}]}}", asList(3)},
                {"{\"query\": {\"or\": [{\"trackingEnabled\": \"true\"}, {\"contentType\": \"AVRO\"}]}}", asList(1, 3, 4)},
        };
    }

    @Test(dataProvider = "topicData")
    public void shouldQueryTopic(String query, List<Integer> positions) {
        // given
        Topic topic1 = operations.buildTopic(topic("testGroup1", "testTopic1").withContentType(AVRO).withTrackingEnabled(false).build());
        Topic topic2 = operations.buildTopic(topic("testGroup1", "testTopic2").withContentType(JSON).withTrackingEnabled(false).build());
        Topic topic3 = operations.buildTopic(topic("testGroup1", "testTopic3").withContentType(AVRO).withTrackingEnabled(true).build());
        Topic topic4 = operations.buildTopic(topic("testGroup2", "testOtherTopic").withContentType(JSON).withTrackingEnabled(true).build());

        List<Topic> topics = asList(topic1, topic2, topic3, topic4);

        // when
        List<Topic> found = management.query().queryTopics(query);

        // then
        assertListMatches(topics, found, positions);
    }

    @DataProvider(name = "subscriptionData")
    public static Object[][] subscriptionData() {
        return new Object[][] {
                {"{\"query\": {}}", asList(1, 2, 3, 4)},
                {"{\"query\": {\"name\": \"subscription1\"}}", asList(1)},
                {"{\"query\": {\"name\": {\"like\": \".*cription1\"}}}", asList(1)},
                {"{\"query\": {\"name\": {\"like\": \"subscript.*\"}}}", asList(1, 2, 4)},
                {"{\"query\": {\"name\": \"subscription1\", \"endpoint\": \"http://endpoint1\"}}", asList(1)},
                {"{\"query\": {\"and\": [{\"name\": \"subscription1\"}, {\"endpoint\": \"http://endpoint1\"}]}}", asList(1)},
                {"{\"query\": {\"or\": [{\"name\": \"subscription1\"}, {\"endpoint\": \"http://endpoint1\"}]}}", asList(1, 3)},
        };
    }

    @Test(dataProvider = "subscriptionData")
    public void shouldQuerySubscription(String query, List<Integer> positions) {
        // given
        Topic topic = operations.buildTopic("NotWise", "NotUsed");

        Subscription subscription1 = operations.createSubscription(topic, enrichSubscription(subscription(topic.getName(), "subscription1"), "http://endpoint1"));
        Subscription subscription2 = operations.createSubscription(topic, enrichSubscription(subscription(topic.getName(), "subscription2"), "http://endpoint2"));
        Subscription subscription3 = operations.createSubscription(topic, enrichSubscription(subscription(topic.getName(), "subTestScription3"), "http://endpoint1"));
        Subscription subscription4 = operations.createSubscription(topic, enrichSubscription(subscription(topic.getName(), "subscription4"), "http://endpoint2"));

        List<Subscription> subscriptions = asList(subscription1, subscription2, subscription3, subscription4);

        // when
        List<Subscription> found = management.query().querySubscriptions(query);

        // then
        assertListMatches(subscriptions, found, positions);
    }

    private Subscription enrichSubscription(SubscriptionBuilder subscription, String endpoint) {
        return subscription
                .withTrackingEnabled(true)
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults().build())
                .withEndpoint(EndpointAddress.of(endpoint))
                .build();
    }

    private <T> void assertListMatches(List<T> elements, List<T> found, List<Integer> positions) {
        found.removeIf(o -> !elements.contains(o));
        List<T> expected = positions.stream().map(i -> elements.get(i - 1)).collect(Collectors.toList());
        assertThat(found).containsOnlyElementsOf(expected);
    }

}
