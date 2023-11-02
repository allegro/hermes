package pl.allegro.tech.hermes.integrationtests.client;

import com.jayway.awaitility.Duration;
import jakarta.ws.rs.core.UriBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;

import java.util.List;

import static com.jayway.awaitility.Awaitility.waitAtMost;

class ManagementTestClient {
    private static final String TOPICS_PATH = "/topics";

    private static final String TOPIC_PATH = "/topics/{topicName}";

    private static final String GROUPS_PATH = "/groups";

    private final WebTestClient webTestClient;

    public ManagementTestClient(String managementContainerUrl) {
        this.webTestClient = WebTestClient
                .bindToServer()
                .baseUrl(managementContainerUrl)
                .build();
    }

    public void createTopic(Topic topic) {
        sendCreateTopicRequest(topic);
        waitUntilTopicCreated(topic);
    }

    public void createGroup(Group group) {
        sendCreateGroupRequest(group);
        waitUntilGroupCreated(group);
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
                        .fromPath(TOPIC_PATH)
                        .build(topicQualifiedName))
                .exchange();
    }

    private List<String> getGroups() {
        return webTestClient.get().uri(GROUPS_PATH)
                .exchange()
                .expectBodyList(String.class)
                .returnResult()
                .getResponseBody();
    }

    private void sendCreateTopicRequest(Topic topic) {
        webTestClient.post().uri(TOPICS_PATH)
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

    private void sendCreateGroupRequest(Group group) {
        webTestClient.post().uri(GROUPS_PATH)
                .body(group, Group.class)
                .exchange()
                .expectStatus()
                .is2xxSuccessful();
    }

    private void waitUntilGroupCreated(Group group) {
        waitAtMost(Duration.TEN_SECONDS)
                .until(() -> getGroups().contains(group.getGroupName()));
    }
}
