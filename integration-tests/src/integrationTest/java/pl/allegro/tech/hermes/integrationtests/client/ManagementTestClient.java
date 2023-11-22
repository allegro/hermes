package pl.allegro.tech.hermes.integrationtests.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.UriBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.OffsetRetransmissionDate;
import pl.allegro.tech.hermes.api.PatchData;
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

    private static final String GROUPS_PATH = "/groups";

    private static final String RETRANSMISSION_PATH = "/topics/{topicName}/subscriptions/{subscriptionName}/retransmission";

    private static final String BLACKLIST_TOPICS_PATH = "/blacklist/topics";

    private static final String BLACKLIST_TOPIC_PATH = "/blacklist/topics/{topicName}";

    private static final String LATEST_UNDELIVERED_MESSAGE = "/topics/{topicName}/subscriptions/{subscriptionName}/undelivered";

    private final WebTestClient webTestClient;

    private final String managementContainerUrl;

    private final ObjectMapper objectMapper;

    public ManagementTestClient(String managementContainerUrl) {
        this.managementContainerUrl = managementContainerUrl;
        this.webTestClient = WebTestClient
                .bindToServer()
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

    public WebTestClient.ResponseSpec getSubscription(String topicQualifiedName, String subscriptionName) {
        return getSingleSubscription(topicQualifiedName, subscriptionName);
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

    WebTestClient.ResponseSpec retransmit(String topicName, String subscriptionName, OffsetRetransmissionDate retransmissionDate) {
        return webTestClient.mutate().responseTimeout(Duration.ofSeconds(29)).build()
                .put().uri(UriBuilder
                        .fromUri(managementContainerUrl)
                        .path(RETRANSMISSION_PATH)
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
}
