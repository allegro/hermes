package pl.allegro.tech.hermes.integrationtests.client;

import com.jayway.awaitility.Duration;
import jakarta.ws.rs.core.UriBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;

import java.util.UUID;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

class ManagementTestClient {
    private static String TOPIC_PATH = "/topics";

    private final WebTestClient webTestClient;

    public ManagementTestClient(String managementContainerUrl) {
        this.webTestClient = WebTestClient
                .bindToServer()
                .baseUrl(managementContainerUrl)
                .build();
    }

    public Topic createRandomTopic() {
        String topicName = UUID.randomUUID().toString();
        String groupName = UUID.randomUUID().toString();

        return createTopic(groupName, topicName);
    }

    public Topic createTopic(String groupName, String topicName) {
        Topic topic = topic(groupName, topicName).build();
        sendCreateTopicRequest(topic);

        waitUntilTopicCreated(topic);
        return topic;
    }

    public Topic getTopic(String groupName, String topicName) {
        return getTopic(groupName + "." + topicName);
    }

    public Topic getTopic(String topicQualifiedName) {
        return getSingleTopic(topicQualifiedName)
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Topic.class)
                .returnResult()
                .getResponseBody();
    }

    private WebTestClient.ResponseSpec getSingleTopic(String topicQualifiedName) {
        return webTestClient.get().uri(UriBuilder
                        .fromPath("topics/{topicName}")
                        .build(topicQualifiedName))
                .exchange();
    }

    private void sendCreateTopicRequest(Topic topic) {
        webTestClient.post().uri(TOPIC_PATH)
                .body(TopicWithSchema.topicWithSchema(topic), TopicWithSchema.class)
                .exchange()
                .expectStatus()
                .is2xxSuccessful();
    }

    private void waitUntilTopicCreated(Topic topic) {
        waitAtMost(Duration.TEN_SECONDS)
                .until(() -> getSingleTopic(topic.getQualifiedName())
                        .expectStatus()
                        .is2xxSuccessful());
    }
}
