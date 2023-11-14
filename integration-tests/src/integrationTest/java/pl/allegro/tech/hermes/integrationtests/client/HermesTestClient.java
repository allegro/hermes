package pl.allegro.tech.hermes.integrationtests.client;

import com.jayway.awaitility.Duration;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;


// TODO remove hermesEndpoint mechanism and dependency to the hermes-api.endpoint module, and pl.allegro.tech.hermes.test.helper.client.Hermes
// TODO use WebTestClient instead, which will be an abstraction for all hermes modules in HermesTestClient.class - https://docs.spring.io/spring-framework/reference/testing/webtestclient.html
// TODO frontend should wait until topic/group created
public class HermesTestClient {
    private final ManagementTestClient managementTestClient;
    private final FrontendTestClient frontendTestClient;

    public HermesTestClient(String managementUrl, String frontendUrl) {
        managementTestClient = new ManagementTestClient(managementUrl);
        frontendTestClient = new FrontendTestClient(frontendUrl);
    }


    // TODO: Include human-readable name. It can be a prefix provided by the developer or test method name.
    // GROUP
    public Group createGroup(String groupName) {
        Group group = Group.from(groupName);
        return createGroupAndWait(group);
    }

    public WebTestClient.ResponseSpec createGroupResponse(Group group) {
        return managementTestClient.createGroup(group);
    }

    public boolean groupExists(Group group) {
        return managementTestClient.getGroups().contains(group.getGroupName());
    }

    // TOPIC
    public Topic createTopic(String groupName, String topicName) {
        return createTopic(topic(groupName, topicName).build());
    }

    public Topic createTopic(Topic topic) {
        return createTopicAndWait(topic, null);
    }

    public TopicWithSchema createTopicWithSchema(Topic topic, String schema) {
        return createTopicWithSchema(TopicWithSchema.topicWithSchema(topic, schema));
    }

    public TopicWithSchema createTopicWithSchema(TopicWithSchema topicWithSchema) {
        createTopicAndWait(topicWithSchema.getTopic(), topicWithSchema.getSchema());
        return topicWithSchema;
    }

    public WebTestClient.ResponseSpec createTopicResponse(Topic topic) {
        return managementTestClient.createTopic(TopicWithSchema.topicWithSchema(topic, null));
    }

    public WebTestClient.ResponseSpec createTopicResponse(TopicWithSchema topicWithSchema) {
        return managementTestClient.createTopic(topicWithSchema);
    }

    public Topic createGroupAndTopic(String groupName, String topicName) {
        createGroup(groupName);
        return createTopic(groupName, topicName);
    }

    public Topic createGroupAndTopic(Topic topic) {
        createGroup(topic.getName().getGroupName());
        return createTopic(topic);
    }

    public Topic getTopic(String topicQualifiedName) {
        return getTopicResponse(topicQualifiedName)
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Topic.class)
                .returnResult()
                .getResponseBody();
    }

    public WebTestClient.ResponseSpec getTopicResponse(String topicQualifiedName) {
        return managementTestClient.getTopic(topicQualifiedName);
    }

    // SUBSCRIPTION
    public Subscription createSubscription(Subscription subscription) {
        return createSubscriptionAndWait(subscription);
    }

    public Subscription getSubscription(String topicQualifiedName, String subscriptionName) {
        return getSubscriptionResponse(topicQualifiedName, subscriptionName)
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Subscription.class)
                .returnResult()
                .getResponseBody();
    }

    public WebTestClient.ResponseSpec getSubscriptionResponse(String topicQualifiedName, String subscriptionName) {
        return managementTestClient.getSubscription(topicQualifiedName, subscriptionName);
    }

    public WebTestClient.ResponseSpec createSubscriptionResponse(String topicQualifiedName, Subscription subscription) {
        return managementTestClient.createSubscription(topicQualifiedName, subscription);
    }

    // PUBLISH
    public WebTestClient.ResponseSpec publishUntilSuccess(String topicQualifiedName, String body) {
        return waitUntilPublished(topicQualifiedName, body);
    }

    public void updateSubscription(Topic topic, String subscription, PatchData patch) {
        managementTestClient.updateSubscription(topic, subscription, patch)
            .expectStatus()
            .is2xxSuccessful();
    }

    // be aware that this method is not waiting for cache refresh in frontends
//    WebTestClient.ResponseSpec publish(String topicQualifiedName, String body) {
//        return frontendTestClient.publish(topicQualifiedName, body);
//    }

    private Group createGroupAndWait(Group group) {
        if (groupExists(group)) {
            return group;
        }
        managementTestClient.createGroup(group)
                .expectStatus()
                .is2xxSuccessful();

        waitUntilGroupCreated(group.getGroupName());
        return group;
    }

    private Topic createTopicAndWait(Topic topic, String schema) {
        managementTestClient.createTopic(TopicWithSchema.topicWithSchema(topic, schema))
                .expectStatus()
                .is2xxSuccessful();
        waitUntilTopicCreated(topic.getQualifiedName());

        return topic;
    }

    private Subscription createSubscriptionAndWait(Subscription subscription) {
        managementTestClient.createSubscription(subscription.getQualifiedTopicName(), subscription)
                .expectStatus()
                .is2xxSuccessful();
        waitUntilSubscriptionCreated(subscription.getQualifiedTopicName(), subscription.getName());

        return subscription;
    }

    private void waitUntilSubscriptionCreated(String topicQualifiedName, String subscriptionName) {
        waitAtMost(Duration.TEN_SECONDS)
                .until(() -> managementTestClient.getSubscription(topicQualifiedName, subscriptionName)
                        .expectStatus()
                        .is2xxSuccessful());
    }

    private void waitUntilTopicCreated(String topicQualifiedName) {
        waitAtMost(Duration.TEN_SECONDS)
                .until(() -> managementTestClient.getTopic(topicQualifiedName)
                        .expectStatus()
                        .is2xxSuccessful());
    }

    private void waitUntilGroupCreated(String groupName) {
        waitAtMost(Duration.TEN_SECONDS)
                .until(() -> managementTestClient.getGroups().contains(groupName));
    }

    private WebTestClient.ResponseSpec waitUntilPublished(String topicQualifiedName, String body) {
        PublisherCallable publisherCallable = new PublisherCallable(frontendTestClient, topicQualifiedName, body);
        waitAtMost(Duration.TEN_SECONDS)
                .until(() -> publisherCallable.call().expectStatus().isCreated());
        return publisherCallable.getResponse();
    }
}
