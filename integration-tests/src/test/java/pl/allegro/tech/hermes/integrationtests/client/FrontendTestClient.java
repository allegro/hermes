package pl.allegro.tech.hermes.integrationtests.client;

import jakarta.ws.rs.core.UriBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

class FrontendTestClient {

    private static final String TOPIC_PATH = "/topics/{topicName}";

    private final WebTestClient webTestClient;
    private final String frontendContainerUrl;

    public FrontendTestClient(String frontendContainerUrl) {
        this.frontendContainerUrl = frontendContainerUrl;
        this.webTestClient = WebTestClient
                .bindToServer()
                .baseUrl(frontendContainerUrl)
                .build();
    }


    public WebTestClient.ResponseSpec publish(String topicQualifiedName, String body) {
        return webTestClient.post().uri(UriBuilder
                        .fromUri(frontendContainerUrl)
                        .path(TOPIC_PATH)
                        .build(topicQualifiedName))
                .body(Mono.just(body), String.class)
                .exchange();
    }
}
