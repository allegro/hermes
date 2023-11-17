package pl.allegro.tech.hermes.integrationtests.client;

import com.jayway.awaitility.Duration;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;

import static com.jayway.awaitility.Awaitility.waitAtMost;


// TODO remove hermesEndpoint mechanism and dependency to the hermes-api.endpoint module, and pl.allegro.tech.hermes.test.helper.client.Hermes
// TODO use WebTestClient instead, which will be an abstraction for all hermes modules in HermesTestClient.class - https://docs.spring.io/spring-framework/reference/testing/webtestclient.html
// TODO frontend should wait until topic/group created
public class HermesTestClient {
    private final ManagementTestClient managementTestClient;
    private final FrontendTestClient frontendTestClient;

    public HermesTestClient(int managementPort, int frontendPort) {
        managementTestClient = new ManagementTestClient(managementPort);
        frontendTestClient = new FrontendTestClient(frontendPort);
    }

    // GROUP

    public WebTestClient.ResponseSpec createGroup(Group group) {
        return managementTestClient.createGroup(group);
    }

    public boolean groupExists(Group group) {
        return managementTestClient.getGroups().contains(group.getGroupName());
    }

    // TOPIC

    public WebTestClient.ResponseSpec createTopic(TopicWithSchema topicWithSchema) {
        return managementTestClient.createTopic(topicWithSchema);
    }

    public WebTestClient.ResponseSpec getTopicResponse(String topicQualifiedName) {
        return managementTestClient.getTopic(topicQualifiedName);
    }

    // SUBSCRIPTION

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

    // PUBLISH
    public WebTestClient.ResponseSpec publishUntilSuccess(String topicQualifiedName, String body) {
        return frontendTestClient.publishUntilSuccess(topicQualifiedName, body);
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

    private void waitUntilSubscriptionCreated(String topicQualifiedName, String subscriptionName) {
        waitAtMost(Duration.TEN_SECONDS)
                .until(() -> managementTestClient.getSubscription(topicQualifiedName, subscriptionName)
                        .expectStatus()
                        .is2xxSuccessful());
    }
}
