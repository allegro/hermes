package pl.allegro.tech.hermes.integrationtests.client;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static jakarta.ws.rs.client.ClientBuilder.newClient;
import static org.awaitility.Awaitility.waitAtMost;
import static org.glassfish.jersey.client.ClientProperties.REQUEST_ENTITY_PROCESSING;
import static org.glassfish.jersey.client.RequestEntityProcessing.CHUNKED;
import static pl.allegro.tech.hermes.api.AvroMediaType.AVRO_BINARY;

public class FrontendTestClient {

    private static final String TOPIC_PATH = "/topics/{topicName}";
    private static final String STATUS_HEALTH_PATH = "/status/health";
    private static final String STATUS_READY_PATH = "/status/ready";

    private final WebTestClient webTestClient;
    private final FrontendSlowClient slowTestClient;
    private final String frontendContainerUrl;
    private final Client chunkedClient;

    public FrontendTestClient(int frontendPort) {
        this.frontendContainerUrl = "http://localhost:" + frontendPort;
        this.webTestClient = WebTestClient
                .bindToServer()
                .baseUrl(frontendContainerUrl)
                .build();
        this.slowTestClient = new FrontendSlowClient("localhost", frontendPort);
        this.chunkedClient = newClient(new ClientConfig().property(REQUEST_ENTITY_PROCESSING, CHUNKED));
    }

    public WebTestClient.ResponseSpec publishUntilSuccess(String topicQualifiedName, String body) {
        AtomicReference<WebTestClient.ResponseSpec> response = new AtomicReference<>();
        waitAtMost(Duration.ofSeconds(10))
                .untilAsserted(() -> response.set(publish(topicQualifiedName, body).expectStatus().isCreated()));
        return response.get();
    }

    public Response publishChunked(String topicQualifiedName, String body) {
        return chunkedClient.target(UriBuilder
                        .fromUri(frontendContainerUrl)
                        .path(TOPIC_PATH)
                        .build(topicQualifiedName))
                .request().post(Entity.text(body));

    }

    public WebTestClient.ResponseSpec publishUntilSuccess(String topicQualifiedName, String body, MultiValueMap<String, String> headers) {
        AtomicReference<WebTestClient.ResponseSpec> response = new AtomicReference<>();
        waitAtMost(Duration.ofSeconds(10))
                .untilAsserted(() -> response.set(publish(topicQualifiedName, body, headers).expectStatus().isCreated()));
        return response.get();
    }

    public WebTestClient.ResponseSpec publishJSONUntilSuccess(String topicQualifiedName, String body, MultiValueMap<String, String> headers) {
        AtomicReference<WebTestClient.ResponseSpec> response = new AtomicReference<>();
        waitAtMost(Duration.ofSeconds(10))
                .untilAsserted(() -> response.set(publishJSON(topicQualifiedName, body, headers).expectStatus().isCreated()));
        return response.get();
    }

    public WebTestClient.ResponseSpec publishAvroUntilSuccess(String topicQualifiedName, byte[] body) {
        return publishAvroUntilSuccess(topicQualifiedName, body, new HttpHeaders());
    }

    public WebTestClient.ResponseSpec publishAvroUntilSuccess(String topicQualifiedName, byte[] body, MultiValueMap<String, String> headers) {
        AtomicReference<WebTestClient.ResponseSpec> response = new AtomicReference<>();
        waitAtMost(Duration.ofSeconds(10))
                .untilAsserted(() -> response.set(publishAvro(topicQualifiedName, body, headers).expectStatus().isCreated()));
        return response.get();
    }

    public WebTestClient.ResponseSpec publishUntilStatus(String topicQualifiedName, String body, int statusCode) {
        AtomicReference<WebTestClient.ResponseSpec> response = new AtomicReference<>();
        waitAtMost(Duration.ofSeconds(10))
                .untilAsserted(() -> response.set(publish(topicQualifiedName, body).expectStatus().isEqualTo(statusCode)));
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

    WebTestClient.ResponseSpec publish(String topicQualifiedName, String body, MultiValueMap<String, String> headers) {
        return webTestClient.post().uri(UriBuilder
                        .fromUri(frontendContainerUrl)
                        .path(TOPIC_PATH)
                        .build(topicQualifiedName))
                .body(Mono.just(body), String.class)
                .headers(requestHeaders -> requestHeaders.addAll(headers))
                .exchange();
    }

    WebTestClient.ResponseSpec publishJSON(String topicQualifiedName, String body, MultiValueMap<String, String> headers) {
        return webTestClient.post().uri(UriBuilder
                        .fromUri(frontendContainerUrl)
                        .path(TOPIC_PATH)
                        .build(topicQualifiedName))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(body), String.class)
                .headers(requestHeaders -> requestHeaders.addAll(headers))
                .exchange();
    }

    WebTestClient.ResponseSpec publishAvro(String topicQualifiedName, byte[] body, MultiValueMap<String, String> headers) {
        return webTestClient.post().uri(UriBuilder
                        .fromUri(frontendContainerUrl)
                        .path(TOPIC_PATH)
                        .build(topicQualifiedName))
                .header("Content-Type", AVRO_BINARY)
                .headers(requestHeaders -> requestHeaders.addAll(headers))
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

    String publishSlowly(int clientTimeout, int pauseTimeBetweenChunks, int delayBeforeSendingFirstData,
                         String topicName, boolean chunkedEncoding) throws IOException, InterruptedException{
        return slowTestClient.slowEvent(clientTimeout, pauseTimeBetweenChunks, delayBeforeSendingFirstData, topicName, chunkedEncoding);
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