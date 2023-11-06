package pl.allegro.tech.hermes.integrationtests.client;

import jakarta.ws.rs.core.UriBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;

class FrontendTestClient {

    private static final String TOPIC_PATH = "/topics/{topicName}";

    private final WebTestClient webTestClient;

    public FrontendTestClient(String frontendContainerUrl) {
        this.webTestClient = WebTestClient
                .bindToServer()
                .baseUrl(frontendContainerUrl)
                .build();
    }


    public WebTestClient.ResponseSpec publish(String topicQualifiedName, String body) {
        return webTestClient.post().uri(UriBuilder.fromPath(TOPIC_PATH).build(topicQualifiedName))
                .body(body, String.class)
                .exchange();
    }
}
