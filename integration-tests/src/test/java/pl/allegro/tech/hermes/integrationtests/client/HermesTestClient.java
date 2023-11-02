package pl.allegro.tech.hermes.integrationtests.client;

import jakarta.ws.rs.core.Response;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMode;
import pl.allegro.tech.hermes.api.Topic;

import java.util.UUID;

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

    public Group createRandomGroup() {
        return createGroup(UUID.randomUUID().toString());
    }

    public Group createGroup(String groupName) {
        Group group = Group.from(groupName);
        managementTestClient.createGroup(group);
        return group;
    }

    public Topic createRandomTopic() {
        String topicName = UUID.randomUUID().toString();
        String groupName = UUID.randomUUID().toString();

        return createTopic(groupName, topicName);
    }

    public Topic createTopic(String groupName, String topicName) {
        Topic topic = topic(groupName, topicName).build();
        managementTestClient.createTopic(topic);
        return topic;
    }

    public Topic createRandomTopicWithGroup() {
        return createTopicWithGroup(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    public Topic createTopicWithGroup(String groupName, String topicName) {
        createGroup(groupName);
        return createTopic(groupName, topicName);
    }

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

    public Response publish(String qualifiedName, String body) {
        return null;
//        return webTarget.path(qualifiedName).request().headers(new MultivaluedHashMap<>(Map.of("Content-Type", MediaType.TEXT_PLAIN)))
//                .post(Entity.entity(body, MediaType.TEXT_PLAIN));
    }

}
