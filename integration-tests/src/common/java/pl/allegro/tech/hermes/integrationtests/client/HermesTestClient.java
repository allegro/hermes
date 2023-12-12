package pl.allegro.tech.hermes.integrationtests.client;

import com.jayway.awaitility.Duration;
import jakarta.ws.rs.core.Response;
import org.springframework.http.HttpHeaders;
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

import java.io.IOException;
import java.util.List;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class HermesTestClient {
    private final ManagementTestClient managementTestClient;
    private final FrontendTestClient frontendTestClient;
    private final ConsumerTestClient consumerTestClient;

    public HermesTestClient(int managementPort, int frontendPort, int consumerPort) {
        this.managementTestClient = new ManagementTestClient(managementPort);
        this.frontendTestClient = new FrontendTestClient(frontendPort);
        this.consumerTestClient = new ConsumerTestClient(consumerPort);
    }

    public WebTestClient.ResponseSpec createGroup(Group group) {
        return managementTestClient.createGroup(group);
    }

    public WebTestClient.ResponseSpec createTopic(TopicWithSchema topicWithSchema) {
        return managementTestClient.createTopic(topicWithSchema);
    }

    public WebTestClient.ResponseSpec getTopicResponse(String topicQualifiedName) {
        return managementTestClient.getTopic(topicQualifiedName);
    }

    public void saveSchema(String topicQualifiedName, boolean validate, String schema) {
        managementTestClient.saveSchema(topicQualifiedName, validate, schema)
                .expectStatus().isCreated();
        waitAtMost(adjust(Duration.ONE_MINUTE)).until(() ->
                managementTestClient.getSchema(topicQualifiedName).expectStatus().isOk()
        );
    }

    public void updateTopic(String qualifiedTopicName, PatchData patch) {
        managementTestClient.updateTopic(qualifiedTopicName, patch)
                .expectStatus()
                .is2xxSuccessful();
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

    public WebTestClient.ResponseSpec getSubscriptionMetrics(String topicQualifiedName, String subscriptionName) {
        return managementTestClient.getSubscriptionMetrics(topicQualifiedName, subscriptionName);
    }

    public void suspendSubscription(Topic topic, String subscription) {
        managementTestClient.updateSubscriptionState(topic, subscription, Subscription.State.SUSPENDED)
                .expectStatus()
                .is2xxSuccessful();
    }

    public void waitUntilSubscriptionActivated(String topicQualifiedName, String subscriptionName) {
        waitAtMost(Duration.TEN_SECONDS)
                .until(() -> managementTestClient.getSubscription(topicQualifiedName, subscriptionName)
                        .expectStatus()
                        .is2xxSuccessful()
                        .expectBody(Subscription.class)
                        .returnResult().getResponseBody().getState().equals(Subscription.State.ACTIVE)
                );
    }

    public void waitUntilSubscriptionSuspended(String topicQualifiedName, String subscriptionName) {
        waitAtMost(Duration.TEN_SECONDS)
                .until(() -> managementTestClient.getSubscription(topicQualifiedName, subscriptionName)
                        .expectStatus()
                        .is2xxSuccessful()
                        .expectBody(Subscription.class)
                        .returnResult().getResponseBody().getState().equals(Subscription.State.SUSPENDED)
                );
    }

    public int publishUntilSuccess(String topicQualifiedName, String body) {
        return frontendTestClient.publishUntilSuccess(topicQualifiedName, body);
    }

    public int publishUntilStatus(String topicQualifiedName, String body, int statusCode) {
        return frontendTestClient.publishUntilStatus(topicQualifiedName, body, statusCode);
    }

    public int publishUntilSuccess(String topicQualifiedName, String body, MultiValueMap<String, String> headers) {
        return frontendTestClient.publishUntilSuccess(topicQualifiedName, body, headers);
    }

    public int publishJSONUntilSuccess(String topicQualifiedName, String body) {
        return frontendTestClient.publishJSONUntilSuccess(topicQualifiedName, body, new HttpHeaders());
    }

    public int publishAvroUntilSuccess(String topicQualifiedName, byte[] body) {
        return frontendTestClient.publishAvroUntilSuccess(topicQualifiedName, body);
    }

    public int publishAvroUntilSuccess(String topicQualifiedName, byte[] body, MultiValueMap<String, String> headers) {
        return frontendTestClient.publishAvroUntilSuccess(topicQualifiedName, body, headers);
    }

    public void updateSubscription(Topic topic, String subscription, PatchData patch) {
        managementTestClient.updateSubscription(topic, subscription, patch)
                .expectStatus()
                .is2xxSuccessful();
    }

    public WebTestClient.ResponseSpec publish(String topicQualifiedName, String body, MultiValueMap<String, String> headers) {
        return frontendTestClient.publishWithHeaders(topicQualifiedName, body, headers);
    }

    public WebTestClient.ResponseSpec publish(String topicQualifiedName, String body) {
        return frontendTestClient.publish(topicQualifiedName, body);
    }

    public WebTestClient.ResponseSpec publishAvro(String topicQualifiedName, byte[] body) {
        return frontendTestClient.publishAvro(topicQualifiedName, body, new HttpHeaders());
    }

    public WebTestClient.ResponseSpec publishAvro(String topicQualifiedName, byte[] body, MultiValueMap<String, String> headers) {
        return frontendTestClient.publishAvro(topicQualifiedName, body, headers);
    }

    public WebTestClient.ResponseSpec publishJSON(String topicQualifiedName, String body) {
        return frontendTestClient.publishJSON(topicQualifiedName, body, new HttpHeaders());
    }

    public Response publishChunked(String topicQualifiedName, String body) {
        return frontendTestClient.publishChunked(topicQualifiedName, body);
    }

    public String publishSlowly(int clientTimeout, int pauseTimeBetweenChunks, int delayBeforeSendingFirstData,
                                String topicName, boolean chunkedEncoding) throws IOException, InterruptedException {
        return frontendTestClient.publishSlowly(clientTimeout, pauseTimeBetweenChunks, delayBeforeSendingFirstData, topicName, chunkedEncoding);
    }

    public String publishSlowly(int clientTimeout, int pauseTimeBetweenChunks, int delayBeforeSendingFirstData, String topicName)
            throws IOException, InterruptedException {
        return publishSlowly(clientTimeout, pauseTimeBetweenChunks, delayBeforeSendingFirstData, topicName, false);
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

    public WebTestClient.ResponseSpec getTopicMetrics(String qualifiedName) {
        return managementTestClient.getTopicMetrics(qualifiedName);
    }

    public WebTestClient.ResponseSpec listSubscriptions(String qualifiedName) {
        return managementTestClient.listSubscriptions(qualifiedName);
    }

    public WebTestClient.ResponseSpec listTopics(String groupName) {
        return managementTestClient.listTopics(groupName);
    }

    public WebTestClient.ResponseSpec getConsumersMetrics() {
        return consumerTestClient.getMetrics();
    }
}
