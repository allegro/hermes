package pl.allegro.tech.hermes.integrationtests.client;

import com.jayway.awaitility.Duration;
import jakarta.ws.rs.core.Response;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMode;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;

import java.util.UUID;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
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


    // TODO: should replace this name with createTopicWithRandomName?
    // TODO: Include human-readable name. It can be a prefix provided by the developer or test method name.

    // GROUP
    public Group createRandomGroup() {
        return createGroup(UUID.randomUUID().toString());
    }

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
    public Topic createRandomTopic() {
        String topicName = UUID.randomUUID().toString();
        String groupName = UUID.randomUUID().toString();

        return createTopic(groupName, topicName);
    }

    public Topic createTopic(String groupName, String topicName) {
        Topic topic = topic(groupName, topicName).build();
        return createTopicAndWait(topic, null);
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

    public Topic createRandomTopicWithGroup() {
        return createTopicWithGroup(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    public Topic createTopicWithGroup(String groupName, String topicName) {
        createGroup(groupName);
        return createTopic(groupName, topicName);
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
    public void createRandomSubscription(Topic topic, String endpoint) {
        Subscription subscription = subscription(topic, UUID.randomUUID().toString())
                .withEndpoint(endpoint)
                .withContentType(ContentType.JSON)
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults().build())
                .withMode(SubscriptionMode.ANYCAST)
                .withState(Subscription.State.ACTIVE)
                .build();
//        endpoints.subscription().create(topic.getQualifiedName(), subscription);
    }

    public void createSubscription(Topic topic, Subscription subscription) {

    }

    // PUBLISH
    public Response publish(String qualifiedName, String body) {
        return null;
//        return webTarget.path(qualifiedName).request().headers(new MultivaluedHashMap<>(Map.of("Content-Type", MediaType.TEXT_PLAIN)))
//                .post(Entity.entity(body, MediaType.TEXT_PLAIN));
    }

    private Group createGroupAndWait(Group group) {
        if (groupExists(group)) {
            return group;
        }
        managementTestClient.createGroup(group)
                .expectStatus()
                .is2xxSuccessful();

        waitUntilGroupCreated(group);
        return group;
    }

    private Topic createTopicAndWait(Topic topic, String schema) {
        managementTestClient.createTopic(TopicWithSchema.topicWithSchema(topic, schema))
                .expectStatus()
                .is2xxSuccessful();
        waitUntilTopicCreated(topic);

        return topic;
    }

    private void waitUntilTopicCreated(Topic topic) {
        waitAtMost(Duration.TEN_SECONDS)
                .until(() -> managementTestClient.getTopic(topic.getQualifiedName())
                        .expectStatus()
                        .is2xxSuccessful());
    }

    private void waitUntilGroupCreated(Group group) {
        waitAtMost(Duration.TEN_SECONDS)
                .until(() -> managementTestClient.getGroups().contains(group.getGroupName()));
    }

}
