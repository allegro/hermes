package pl.allegro.tech.hermes.test.helper.client.integration;

import jakarta.ws.rs.core.Response;
import java.time.Duration;
import org.assertj.core.api.Assertions;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import pl.allegro.tech.hermes.api.BlacklistStatus;
import pl.allegro.tech.hermes.api.ConsumerGroup;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.MessageFiltersVerificationInput;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;
import pl.allegro.tech.hermes.api.OffsetRetransmissionDate;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.consumers.supervisor.process.RunningSubscriptionStatus;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
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

    public HermesTestClient(int managementPort, int frontendPort, int consumerPort, String defaultHeaderName, String defaultHeaderValue) {
        this.managementTestClient = new ManagementTestClient(managementPort, defaultHeaderName, defaultHeaderValue);
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

    public WebTestClient.ResponseSpec saveSchema(String topicQualifiedName, String schema) {
        return managementTestClient.saveSchema(topicQualifiedName, schema);

    }

    public void ensureSchemaSaved(String topicQualifiedName, boolean validate, String schema) {
        managementTestClient.saveSchema(topicQualifiedName, validate, schema)
                .expectStatus().isCreated();
        waitAtMost(adjust(Duration.ofMinutes(1))).untilAsserted(() ->
                managementTestClient.getSchema(topicQualifiedName).expectStatus().isOk()
        );
    }

    public WebTestClient.ResponseSpec saveSchema(String topicQualifiedName, boolean validate, String schema) {
        return managementTestClient.saveSchema(topicQualifiedName, validate, schema);
    }

    public WebTestClient.ResponseSpec getSchema(String topicQualifiedName) {
        return managementTestClient.getSchema(topicQualifiedName);
    }

    public WebTestClient.ResponseSpec deleteSchema(String topicQualifiedName) {
        return managementTestClient.deleteSchema(topicQualifiedName);
    }

    public WebTestClient.ResponseSpec updateTopic(String qualifiedTopicName, PatchData patch) {
        return managementTestClient.updateTopic(qualifiedTopicName, patch);
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

    public WebTestClient.ResponseSpec suspendSubscription(Topic topic, String subscription) {
        return managementTestClient.updateSubscriptionState(topic, subscription, Subscription.State.SUSPENDED)
                .expectStatus()
                .is2xxSuccessful();
    }

    public void waitUntilSubscriptionActivated(String topicQualifiedName, String subscriptionName) {
        waitAtMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                            assertThat(managementTestClient.getSubscription(topicQualifiedName, subscriptionName)
                                    .expectStatus()
                                    .is2xxSuccessful()
                                    .expectBody(Subscription.class)
                                    .returnResult().getResponseBody().getState())
                                    .isEqualTo(Subscription.State.ACTIVE);
                            assertThat(managementTestClient.getConsumerGroupsDescription(topicQualifiedName, subscriptionName)
                                    .expectBodyList(ConsumerGroup.class).returnResult().getResponseBody()
                                    .get(0)
                                    .getState())
                                    .isEqualTo("Stable");
                        }
                );
    }

    public void waitUntilSubscriptionSuspended(String topicQualifiedName, String subscriptionName) {
        waitAtMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                            assertThat(managementTestClient.getSubscription(topicQualifiedName, subscriptionName)
                                    .expectStatus()
                                    .is2xxSuccessful()
                                    .expectBody(Subscription.class)
                                    .returnResult().getResponseBody().getState())
                                    .isEqualTo(Subscription.State.SUSPENDED);
                            assertThat(managementTestClient.getConsumerGroupsDescription(topicQualifiedName, subscriptionName)
                                    .expectBodyList(ConsumerGroup.class).returnResult().getResponseBody()
                                    .get(0)
                                    .getState())
                                    .isEqualTo("Empty");
                        }
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

    public WebTestClient.ResponseSpec updateSubscription(Topic topic, String subscription, PatchData patch) {
        return managementTestClient.updateSubscription(topic, subscription, patch);
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

    public WebTestClient.ResponseSpec getPreview(String qualifiedTopicName) {
        return managementTestClient.getPreview(qualifiedTopicName);
    }

    public WebTestClient.ResponseSpec getTopicMetrics(String qualifiedName) {
        return managementTestClient.getTopicMetrics(qualifiedName);
    }

    public WebTestClient.ResponseSpec listSubscriptions(String qualifiedName) {
        return managementTestClient.listSubscriptions(qualifiedName, false);
    }

    public WebTestClient.ResponseSpec listTopics(String groupName) {
        return managementTestClient.listTopics(groupName, false);
    }

    public WebTestClient.ResponseSpec getConsumersMetrics() {
        return consumerTestClient.getMetrics();
    }

    public WebTestClient.ResponseSpec getFrontendMetrics() {
        return frontendTestClient.getMetrics();
    }

    public WebTestClient.ResponseSpec verifyFilters(String qualifiedTopicName,
                                                    MessageFiltersVerificationInput input) {
        return managementTestClient.verifyFilters(qualifiedTopicName, input);
    }

    public WebTestClient.ResponseSpec getManagementHealth() {
        return managementTestClient.getStatusHealth();
    }

    public WebTestClient.ResponseSpec getManagementStats() {
        return managementTestClient.getStats();
    }

    public WebTestClient.ResponseSpec setReadiness(String dc, boolean state) {
        return managementTestClient.setReadiness(dc, state);
    }

    public WebTestClient.ResponseSpec getReadiness() {
        return managementTestClient.getReadiness();
    }

    public WebTestClient.ResponseSpec getFrontendReadiness() {
        return frontendTestClient.getStatusReady();
    }

    public WebTestClient.ResponseSpec getAllTopicClients(String topicQualifiedName) {
        return managementTestClient.getAllTopicClients(topicQualifiedName);
    }

    public WebTestClient.ResponseSpec getSubscriptionsForOwner(String source, String ownerId) {
        return managementTestClient.getSubscriptionsForOwner(source, ownerId);
    }

    public WebTestClient.ResponseSpec deleteSubscription(String topicQualifiedName, String subscriptionName) {
        return managementTestClient.deleteSubscription(topicQualifiedName, subscriptionName);
    }

    public WebTestClient.ResponseSpec getTopicsForOwner(String source, String ownerId) {
        return managementTestClient.getTopicsForOwner(source, ownerId);
    }

    public WebTestClient.ResponseSpec deleteTopic(String topicQualifiedName) {
        return managementTestClient.deleteTopic(topicQualifiedName);
    }

    public WebTestClient.ResponseSpec listTrackedTopics(String groupName) {
        return managementTestClient.listTopics(groupName, true);
    }

    public WebTestClient.ResponseSpec queryTopics(String group, String query) {
        return managementTestClient.queryTopics(group, query);
    }

    public WebTestClient.ResponseSpec queryGroups(String query) {
        return managementTestClient.queryGroups(query);
    }

    public WebTestClient.ResponseSpec queryTopics(String query) {
        return managementTestClient.queryTopics(query);
    }

    public WebTestClient.ResponseSpec queryTopicMetrics(String query) {
        return managementTestClient.queryTopicMetrics(query);
    }

    public WebTestClient.ResponseSpec querySubscriptionMetrics(String query) {
        return managementTestClient.querySubscriptionMetrics(query);
    }

    public WebTestClient.ResponseSpec querySubscriptions(String query) {
        return managementTestClient.querySubscriptions(query);
    }

    public WebTestClient.ResponseSpec listUnhealthy() {
        return managementTestClient.listUnhealthy();
    }

    public WebTestClient.ResponseSpec listUnhealthyAsPlainText() {
        return managementTestClient.listUnhealthyAsPlainText();
    }

    public WebTestClient.ResponseSpec listUnhealthyForOwner(String ownerId) {
        return managementTestClient.listUnhealthy(ownerId);
    }

    public WebTestClient.ResponseSpec listUnhealthyForOwnerAsPlainText(String ownerId) {
        return managementTestClient.listUnhealthyAsPlainText(ownerId);
    }

    public WebTestClient.ResponseSpec listUnhealthyForTopic(String qualifiedName) {
        return managementTestClient.listUnhealthyForTopic(qualifiedName);
    }

    public WebTestClient.ResponseSpec listUnhealthyForTopicAsPlainText(String qualifiedName) {
        return managementTestClient.listUnhealthyForTopicAsPlainText(qualifiedName);
    }

    public WebTestClient.ResponseSpec listUnhealthyForSubscription(String topicQualifiedName, String subscriptionName) {
        return managementTestClient.listUnhealthyForSubscription(topicQualifiedName, subscriptionName);
    }

    public WebTestClient.ResponseSpec listUnhealthyForSubscriptionAsPlainText(String topicQualifiedName, String subscriptionName) {
        return managementTestClient.listUnhealthyForSubscriptionAsPlainText(topicQualifiedName, subscriptionName);
    }

    public WebTestClient.ResponseSpec createOAuthProvider(OAuthProvider provider) {
        return managementTestClient.createOAuthProvider(provider);
    }

    public WebTestClient.ResponseSpec getOAuthProvider(String name) {
        return managementTestClient.getOAuthProvider(name);
    }

    public WebTestClient.ResponseSpec removeOAuthProvider(String name) {
        return managementTestClient.removeOAuthProvider(name);
    }

    public WebTestClient.ResponseSpec listOAuthProvider() {
        return managementTestClient.listOAuthProvider();
    }

    public WebTestClient.ResponseSpec updateOAuthProvider(String name, PatchData patch) {
        return managementTestClient.updateOAuthProvider(name, patch);
    }

    public WebTestClient.ResponseSpec searchOwners(String source, String searchString) {
        return managementTestClient.searchOwners(source, searchString);
    }

    public WebTestClient.ResponseSpec setMode(String mode) {
        return managementTestClient.setMode(mode);
    }

    public WebTestClient.ResponseSpec getOfflineRetransmissionTasks() {
        return managementTestClient.getOfflineRetransmissionTasks();
    }

    public WebTestClient.ResponseSpec deleteOfflineRetransmissionTask(String taskId) {
        return managementTestClient.deleteOfflineRetransmissionTask(taskId);
    }

    public WebTestClient.ResponseSpec createOfflineRetransmissionTask(OfflineRetransmissionRequest request) {
        return managementTestClient.createOfflineRetransmissionTask(request);
    }

    public WebTestClient.ResponseSpec createSubscription(Subscription subscription) {
        return managementTestClient.createSubscription(subscription);
    }

    public WebTestClient.ResponseSpec listTrackedSubscriptions(String qualifiedName) {
        return managementTestClient.listSubscriptions(qualifiedName, true);
    }

    public WebTestClient.ResponseSpec querySubscriptions(String qualifiedName, String query) {
        return managementTestClient.querySubscriptions(qualifiedName, query);
    }

    public WebTestClient.ResponseSpec getSubscriptionHealth(String qualifiedTopicName, String name) {
        return managementTestClient.getSubscriptionHealth(qualifiedTopicName, name);
    }

    public WebTestClient.ResponseSpec getConsumerGroupsDescription(String qualifiedTopicName, String subscriptionName) {
        return managementTestClient.getConsumerGroupsDescription(qualifiedTopicName, subscriptionName);
    }

    public WebTestClient.ResponseSpec deleteGroup(String groupName) {
        return managementTestClient.deleteGroup(groupName);
    }

    public WebTestClient.ResponseSpec updateGroup(String groupName, Group group) {
        return managementTestClient.updateGroup(groupName, group);
    }

    public List<String> getGroups() {
        return managementTestClient.getGroups();
    }

    public WebTestClient.ResponseSpec moveOffsetsToTheEnd(String topicQualifiedName, String subscriptionName) {
        return managementTestClient.moveOffsetsToTheEnd(topicQualifiedName, subscriptionName);
    }
}
