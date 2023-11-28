package pl.allegro.tech.hermes.integrationtests.client;

import jakarta.ws.rs.core.UriBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.waitAtMost;

public class FrontendTestClient {

    private static final String TOPIC_PATH = "/topics/{topicName}";
    private static final String STATUS_HEALTH_PATH = "/status/health";
    private static final String STATUS_READY_PATH = "/status/ready";

    private final WebTestClient webTestClient;
    private final String frontendContainerUrl;

    public FrontendTestClient(int frontendPort) {
        this.frontendContainerUrl = "http://localhost:" + frontendPort;
        this.webTestClient = WebTestClient
                .bindToServer()
                .baseUrl(frontendContainerUrl)
                .build();
    }

    public WebTestClient.ResponseSpec publishUntilSuccess(String topicQualifiedName, String body) {
        AtomicReference<WebTestClient.ResponseSpec> response = new AtomicReference<>();
        waitAtMost(Duration.ofSeconds(10))
                .untilAsserted(() -> response.set(publish(topicQualifiedName, body).expectStatus().isCreated()));
        return response.get();
    }

    public WebTestClient.ResponseSpec publishUntilSuccess(String topicQualifiedName, byte[] body) {
        AtomicReference<WebTestClient.ResponseSpec> response = new AtomicReference<>();
        waitAtMost(Duration.ofSeconds(10))
                .untilAsserted(() -> response.set(publish(topicQualifiedName, body).expectStatus().isCreated()));
        return response.get();
    }

    WebTestClient.ResponseSpec publish(String topicQualifiedName, String body) {
        return webTestClient.post().uri(UriBuilder
                        .fromUri(frontendContainerUrl)
                        .path(TOPIC_PATH)
                        .build(topicQualifiedName))
                .body(Mono.just(body), String.class)
                .exchange();
    }

    WebTestClient.ResponseSpec publish(String topicQualifiedName, byte[] body) {
        return webTestClient.post().uri(UriBuilder
                        .fromUri(frontendContainerUrl)
                        .path(TOPIC_PATH)
                        .build(topicQualifiedName))
                .body(Mono.just(body), byte[].class)
                .exchange();
    }

    WebTestClient.ResponseSpec publishWithHeaders(String topicQualifiedName, String body, MultiValueMap<String, String> headers) {
        return webTestClient.post().uri(UriBuilder
                        .fromUri(frontendContainerUrl)
                        .path(TOPIC_PATH)
                        .build(topicQualifiedName))
                .headers(it -> it.addAll(headers))
                .body(Mono.just(body), String.class)
                .exchange();
    }

    public WebTestClient.ResponseSpec getStatusHealth() {
        return webTestClient.get().uri(UriBuilder
                        .fromUri(frontendContainerUrl)
                        .path(STATUS_HEALTH_PATH)
                        .build())
                .exchange();
    }

    public WebTestClient.ResponseSpec getStatusReady() {
        return webTestClient.get().uri(UriBuilder
                        .fromUri(frontendContainerUrl)
                        .path(STATUS_READY_PATH)
                        .build())
                .exchange();
    }
}
