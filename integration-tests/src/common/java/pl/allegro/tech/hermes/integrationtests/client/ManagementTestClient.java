package pl.allegro.tech.hermes.integrationtests.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.UriBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.OffsetRetransmissionDate;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Readiness;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

public class ManagementTestClient {
    private static final String TOPICS_PATH = "/topics";

    private static final String TOPIC_PATH = "/topics/{topicName}";

    private static final String SUBSCRIPTIONS_PATH = "/topics/{topicName}/subscriptions";

    private static final String SUBSCRIPTION_PATH = "/topics/{topicName}/subscriptions/{subscriptionName}";

    private static final String SUBSCRIPTION_STATE_PATH = "/topics/{topicName}/subscriptions/{subscriptionName}/state";

    private static final String SUBSCRIPTION_METRICS_PATH = "/topics/{topicName}/subscriptions/{subscriptionName}/metrics";

    private static final String GROUPS_PATH = "/groups";

    private static final String RETRANSMISSION_PATH = "/topics/{topicName}/subscriptions/{subscriptionName}/retransmission";

    private static final String BLACKLIST_TOPICS_PATH = "/blacklist/topics";

    private static final String BLACKLIST_TOPIC_PATH = "/blacklist/topics/{topicName}";

    private static final String LATEST_UNDELIVERED_MESSAGE = "/topics/{topicName}/subscriptions/{subscriptionName}/undelivered";

    private static final String TOPIC_PREVIEW = "/topics/{topicName}/preview/cluster/{brokersClusterName}/partition/{partition}/offset/{offset}";

    private static final String SET_READINESS = "/readiness/datacenters/{dc}";

    private static final String TOPIC_SCHEMA = "topics/{topicName}/schema";

    private final WebTestClient webTestClient;

    private final String managementContainerUrl;

    private final ObjectMapper objectMapper;

    public ManagementTestClient(int managementPort) {
        this.managementContainerUrl = "http://localhost:" + managementPort;
        this.webTestClient = WebTestClient
                .bindToServer()
                .responseTimeout(Duration.ofSeconds(30))
                .baseUrl(managementContainerUrl)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public WebTestClient.ResponseSpec createGroup(Group group) {
        return sendCreateGroupRequest(group);
    }

    public List<String> getGroups() {
        String jsonString = webTestClient.get().uri(GROUPS_PATH)
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        return mapStringJsonToListOfString(jsonString);
    }

    public WebTestClient.ResponseSpec createTopic(TopicWithSchema topicWithSchema) {
        return sendCreateTopicRequest(topicWithSchema);
    }

    public WebTestClient.ResponseSpec getTopic(String topicQualifiedName) {
        return getSingleTopic(topicQualifiedName);
    }

    public WebTestClient.ResponseSpec createSubscription(Subscription subscription) {
        return sendCreateSubscriptionRequest(subscription);
    }

    public WebTestClient.ResponseSpec updateSubscription(Topic topic, String subscription, PatchData patch) {
        return webTestClient.put().uri(UriBuilder
                        .fromUri(managementContainerUrl)
                        .path(SUBSCRIPTION_PATH)
                        .build(topic.getQualifiedName(), subscription))
                .body(Mono.just(patch), PatchData.class)
                .exchange();
    }

    public WebTestClient.ResponseSpec updateSubscriptionState(Topic topic, String subscription, Subscription.State state) {
        return webTestClient.put().uri(UriBuilder
                        .fromUri(managementContainerUrl)
                        .path(SUBSCRIPTION_STATE_PATH)
                        .build(topic.getQualifiedName(), subscription))
                .body(Mono.just(state), Subscription.State.class)
                .exchange();
    }

    public WebTestClient.ResponseSpec getSubscription(String topicQualifiedName, String subscriptionName) {
        return getSingleSubscription(topicQualifiedName, subscriptionName);
    }

    public WebTestClient.ResponseSpec getSubscriptionMetrics(String topicQualifiedName, String subscriptionName) {
        return webTestClient.get().uri(UriBuilder
                        .fromUri(managementContainerUrl)
                        .path(SUBSCRIPTION_METRICS_PATH)
                        .build(topicQualifiedName, subscriptionName))
                .exchange();
    }

    private WebTestClient.ResponseSpec getSingleTopic(String topicQualifiedName) {
        return webTestClient.get().uri(
                        UriBuilder.fromUri(managementContainerUrl)
                                .path(TOPIC_PATH)
                                .build(topicQualifiedName))
                .exchange();
    }

    private WebTestClient.ResponseSpec getSingleSubscription(String topicQualifiedName, String subscriptionName) {
        return webTestClient.get().uri(UriBuilder
                        .fromUri(managementContainerUrl)
                        .path(SUBSCRIPTION_PATH)
                        .build(topicQualifiedName, subscriptionName))
                .exchange();
    }

    private WebTestClient.ResponseSpec sendCreateTopicRequest(TopicWithSchema topicWithSchema) {
        return webTestClient.post().uri(TOPICS_PATH)
                .body(Mono.just(topicWithSchema), TopicWithSchema.class)
                .exchange();
    }

    WebTestClient.ResponseSpec retransmit(String topicName, String subscriptionName, OffsetRetransmissionDate retransmissionDate, boolean dryRun) {
        return webTestClient.put().uri(UriBuilder
                        .fromUri(managementContainerUrl)
                        .path(RETRANSMISSION_PATH)
                        .queryParam("dryRun", dryRun)
                        .build(topicName, subscriptionName))
                .body(Mono.just(retransmissionDate), OffsetRetransmissionDate.class)
                .exchange();
    }

    private WebTestClient.ResponseSpec sendCreateSubscriptionRequest(Subscription subscription) {
        return webTestClient.post().uri(UriBuilder
                        .fromUri(managementContainerUrl)
                        .path(SUBSCRIPTIONS_PATH)
                        .build(subscription.getQualifiedTopicName()))
                .body(Mono.just(subscription), Subscription.class)
                .exchange();
    }

    private WebTestClient.ResponseSpec sendCreateGroupRequest(Group group) {
        return webTestClient.post().uri(GROUPS_PATH)
                .body(Mono.just(group), Group.class)
                .exchange();
    }

    private List<String> mapStringJsonToListOfString(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public WebTestClient.ResponseSpec blacklistTopic(String topicQualifiedName) {
        return webTestClient.post().uri(BLACKLIST_TOPICS_PATH)
                .body(Mono.just(List.of(topicQualifiedName)), List.class)
                .exchange();
    }

    public WebTestClient.ResponseSpec unblacklistTopic(String topicQualifiedName) {
        return webTestClient.delete().uri(UriBuilder.fromUri(managementContainerUrl)
                        .path(BLACKLIST_TOPIC_PATH)
                        .build(topicQualifiedName))
                .exchange();
    }

    public WebTestClient.ResponseSpec isTopicBlacklisted(String topicQualifiedName) {
        return webTestClient.get().uri(UriBuilder.fromUri(managementContainerUrl)
                        .path(BLACKLIST_TOPIC_PATH)
                        .build(topicQualifiedName))
                .exchange();
    }

    public WebTestClient.ResponseSpec getLatestUndeliveredMessage(String topicQualifiedName, String subscriptionName) {
        return webTestClient.get().uri(UriBuilder.fromUri(managementContainerUrl)
                        .path(LATEST_UNDELIVERED_MESSAGE)
                        .build(topicQualifiedName, subscriptionName))
                .exchange();
    }

    public WebTestClient.ResponseSpec getPreview(String qualifiedTopicName, String primaryKafkaClusterName, int partition, long offset) {
        return webTestClient.get().uri(UriBuilder.fromUri(managementContainerUrl)
                        .path(TOPIC_PREVIEW)
                        .build(qualifiedTopicName, primaryKafkaClusterName, partition, offset))
                .exchange();
    }

    public WebTestClient.ResponseSpec updateTopic(String qualifiedTopicName, PatchData patch) {
        return webTestClient.put().uri(UriBuilder
                        .fromUri(managementContainerUrl)
                        .path(TOPIC_PATH)
                        .build(qualifiedTopicName))
                .body(Mono.just(patch), PatchData.class)
                .exchange();
    }

    public WebTestClient.ResponseSpec setReadiness(String dc, boolean state) {
        return webTestClient.post().uri(UriBuilder
                        .fromUri(managementContainerUrl)
                        .path(SET_READINESS)
                        .build(dc))
                .body(Mono.just(new Readiness(state)), Readiness.class)
                .exchange();
    }

    public WebTestClient.ResponseSpec saveSchema(String qualifiedTopicName, boolean validate, String schema) {
        return webTestClient.post().uri(UriBuilder
                        .fromUri(managementContainerUrl)
                        .path(TOPIC_SCHEMA)
                        .queryParam("validate", validate)
                        .build(qualifiedTopicName))
                .header("Content-Type", "application/json")
                .body(Mono.just(schema), String.class)
                .exchange();
    }

    public WebTestClient.ResponseSpec getSchema(String qualifiedTopicName) {
        return webTestClient.get().uri(UriBuilder
                        .fromUri(managementContainerUrl)
                        .path(TOPIC_SCHEMA)
                        .build(qualifiedTopicName))
                .exchange();
    }
}
