package pl.allegro.tech.hermes.integrationtests.client;

import com.jayway.awaitility.Duration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import pl.allegro.tech.hermes.api.BlacklistStatus;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.OffsetRetransmissionDate;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.consumers.supervisor.process.RunningSubscriptionStatus;

import java.util.List;

import static com.jayway.awaitility.Awaitility.waitAtMost;

public class HermesTestClient {
    private final ManagementTestClient managementTestClient;
    private final FrontendTestClient frontendTestClient;
    private final ConsumerTestClient consumerTestClient;

    public HermesTestClient(int managementPort, int frontendPort, int consumerPort) {
        this.managementTestClient = new ManagementTestClient(managementPort);
        this.frontendTestClient = new FrontendTestClient(frontendPort);
        this.consumerTestClient = new ConsumerTestClient(consumerPort);
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


    public WebTestClient.ResponseSpec publishWithHeaders(String topicQualifiedName, String body, MultiValueMap<String, String> headers) {
        return frontendTestClient.publishWithHeaders(topicQualifiedName, body, headers);
    }

    public WebTestClient.ResponseSpec publishUntilSuccess(String topicQualifiedName, byte[] body) {
        return frontendTestClient.publishUntilSuccess(topicQualifiedName, body);
    }

    public void updateSubscription(Topic topic, String subscription, PatchData patch) {
        managementTestClient.updateSubscription(topic, subscription, patch)
            .expectStatus()
            .is2xxSuccessful();
    }

    public WebTestClient.ResponseSpec publish(String topicQualifiedName, String body) {
        return frontendTestClient.publish(topicQualifiedName, body);
    }

    private void waitUntilSubscriptionCreated(String topicQualifiedName, String subscriptionName) {
        waitAtMost(Duration.TEN_SECONDS)
                .until(() -> managementTestClient.getSubscription(topicQualifiedName, subscriptionName)
                        .expectStatus()
                        .is2xxSuccessful());
    }

    public void blacklistTopic(String topicQualifiedName) {
        managementTestClient.blacklistTopic(topicQualifiedName).expectStatus().is2xxSuccessful();
    }

    public WebTestClient.ResponseSpec blacklistTopicResponse(String topicQualifiedName) {
        return managementTestClient.blacklistTopic(topicQualifiedName);
    }

    public void unblacklistTopic(String topicQualifiedName) {
        managementTestClient.unblacklistTopic(topicQualifiedName).expectStatus().is2xxSuccessful();
    }

    public BlacklistStatus isTopicBlacklisted(String topicQualifiedName) {
        return managementTestClient.isTopicBlacklisted(topicQualifiedName)
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(BlacklistStatus.class)
                .returnResult()
                .getResponseBody();
    }

    public WebTestClient.ResponseSpec unblacklistTopicResponse(String topicQualifiedName) {
        return managementTestClient.unblacklistTopic(topicQualifiedName);
    }

    public WebTestClient.ResponseSpec getLatestUndeliveredMessage(String topicQualifiedName, String subscriptionName) {
        return managementTestClient.getLatestUndeliveredMessage(topicQualifiedName, subscriptionName);
    }

    public List<RunningSubscriptionStatus> getRunningSubscriptionsStatus() {
        return consumerTestClient.getRunningSubscriptionsStatus()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(RunningSubscriptionStatus.class)
                .returnResult()
                .getResponseBody();
    }

    public WebTestClient.ResponseSpec retransmit(String qualifiedName, String subscriptionName, OffsetRetransmissionDate retransmissionDate, boolean dryRun) {
        return managementTestClient.retransmit(qualifiedName, subscriptionName, retransmissionDate, dryRun);
    }

    public WebTestClient.ResponseSpec getPreview(String qualifiedTopicName, String primaryKafkaClusterName, int partition, long offset) {
        return managementTestClient.getPreview(qualifiedTopicName, primaryKafkaClusterName, partition, offset);
    }
}
