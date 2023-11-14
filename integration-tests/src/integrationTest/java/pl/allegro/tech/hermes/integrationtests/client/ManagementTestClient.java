package pl.allegro.tech.hermes.integrationtests.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.UriBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import reactor.core.publisher.Mono;

import java.util.List;

class ManagementTestClient {
    private static final String TOPICS_PATH = "/topics";

    private static final String BLACKLIST_TOPICS_PATH = "/blacklist/topics";

    private static final String BLACKLIST_TOPIC_PATH = "/blacklist/topics/{topicName}";

    private static final String LATEST_UNDELIVERED_MESSAGE = "/topics/{topicName}/subscriptions/{subscriptionName}/undelivered";

    private static final String TOPIC_PATH = "/topics/{topicName}";

    private static final String SUBSCRIPTIONS_PATH = "/topics/{topicName}/subscriptions";

    private static final String SUBSCRIPTION_PATH = "/topics/{topicName}/subscriptions/{subscriptionName}";

    private static final String GROUPS_PATH = "/groups";

    private final WebTestClient webTestClient;

    private final String managementContaiNerUrl;

    private final ObjectMapper objectMapper;

    public ManagementTestClient(String managementContainerUrl) {
        this.managementContaiNerUrl = managementContainerUrl;
        this.webTestClient = WebTestClient
                .bindToServer()
                .baseUrl(managementContainerUrl)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public WebTestClient.ResponseSpec createGroup(Group group) {
        return sendCreateGroupRequest(group);
    }

    protected List<String> getGroups() {
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

    public WebTestClient.ResponseSpec createSubscription(String topicQualifiedName, Subscription subscription) {
        return sendCreateSubscriptionRequest(topicQualifiedName, subscription);
    }

    public WebTestClient.ResponseSpec getSubscription(String topicQualifiedName, String subscriptionName) {
        return getSingleSubscription(topicQualifiedName, subscriptionName);
    }

    public WebTestClient.ResponseSpec blacklistTopic(String topicQualifiedName) {
        return webTestClient.post().uri(BLACKLIST_TOPICS_PATH)
                .body(Mono.just(List.of(topicQualifiedName)), List.class)
                .exchange();
    }

    public WebTestClient.ResponseSpec unblacklistTopic(String topicQualifiedName) {
        return webTestClient.delete().uri(UriBuilder.fromUri(managementContaiNerUrl)
                        .path(BLACKLIST_TOPIC_PATH)
                        .build(topicQualifiedName))
                .exchange();
    }

    public WebTestClient.ResponseSpec isTopicBlacklisted(String topicQualifiedName) {
        return webTestClient.get().uri(UriBuilder.fromUri(managementContaiNerUrl)
                        .path(BLACKLIST_TOPIC_PATH)
                        .build(topicQualifiedName))
                .exchange();
    }

    public WebTestClient.ResponseSpec getLatestUndeliveredMessage(String topicQualifiedName, String subscriptionName) {
        return webTestClient.get().uri(UriBuilder.fromUri(managementContaiNerUrl)
                        .path(LATEST_UNDELIVERED_MESSAGE)
                        .build(topicQualifiedName, subscriptionName))
                .exchange();
    }

    private WebTestClient.ResponseSpec getSingleTopic(String topicQualifiedName) {
        return webTestClient.get().uri(
                        UriBuilder.fromUri(managementContaiNerUrl)
                                .path(TOPIC_PATH)
                                .build(topicQualifiedName))
                .exchange();
    }

    private WebTestClient.ResponseSpec getSingleSubscription(String topicQualifiedName, String subscriptionName) {
        return webTestClient.get().uri(UriBuilder
                        .fromUri(managementContaiNerUrl)
                        .path(SUBSCRIPTION_PATH)
                        .build(topicQualifiedName, subscriptionName))
                .exchange();
    }

    private WebTestClient.ResponseSpec sendCreateTopicRequest(TopicWithSchema topicWithSchema) {
        return webTestClient.post().uri(TOPICS_PATH)
                .body(Mono.just(topicWithSchema), TopicWithSchema.class)
                .exchange();
    }

    private WebTestClient.ResponseSpec sendCreateSubscriptionRequest(String topicQualifiedName, Subscription subscription) {
        return webTestClient.post().uri(UriBuilder
                        .fromUri(managementContaiNerUrl)
                        .path(SUBSCRIPTIONS_PATH)
                        .build(topicQualifiedName))
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
}
