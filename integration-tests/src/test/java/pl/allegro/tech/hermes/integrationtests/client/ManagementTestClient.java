package pl.allegro.tech.hermes.integrationtests.client;

import jakarta.ws.rs.core.UriBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.TopicWithSchema;

import java.util.List;

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

    public WebTestClient.ResponseSpec createGroup(Group group) {
        return sendCreateGroupRequest(group);
    }

    protected List<String> getGroups() {
        return webTestClient.get().uri(GROUPS_PATH)
                .exchange()
                .expectBodyList(String.class)
                .returnResult()
                .getResponseBody();
    }

    public WebTestClient.ResponseSpec createTopic(TopicWithSchema topicWithSchema) {
        return sendCreateTopicRequest(topicWithSchema);
    }

    public WebTestClient.ResponseSpec getTopic(String topicQualifiedName) {
        return getSingleTopic(topicQualifiedName);
    }

    private WebTestClient.ResponseSpec getSingleTopic(String topicQualifiedName) {
        return webTestClient.get().uri(UriBuilder
                        .fromPath(TOPIC_PATH)
                        .build(topicQualifiedName))
                .exchange();
    }

    private WebTestClient.ResponseSpec sendCreateTopicRequest(TopicWithSchema topicWithSchema) {
        return webTestClient.post().uri(TOPICS_PATH)
                .body(topicWithSchema, TopicWithSchema.class)
                .exchange();
    }

    private WebTestClient.ResponseSpec sendCreateGroupRequest(Group group) {
        return webTestClient.post().uri(GROUPS_PATH)
                .body(group, Group.class)
                .exchange();
    }
}
