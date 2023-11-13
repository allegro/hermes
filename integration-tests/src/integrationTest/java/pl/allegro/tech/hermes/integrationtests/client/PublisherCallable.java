package pl.allegro.tech.hermes.integrationtests.client;

import org.springframework.test.web.reactive.server.WebTestClient;

final class PublisherCallable {

    private WebTestClient.ResponseSpec response;

    private final FrontendTestClient frontendTestClient;

    private final String topicQualifiedName;

    private final String body;

    public PublisherCallable(FrontendTestClient frontendTestClient, String topicQualifiedName, String body) {
        this.frontendTestClient = frontendTestClient;
        this.topicQualifiedName = topicQualifiedName;
        this.body = body;
    }

    public WebTestClient.ResponseSpec call() {
        this.response = frontendTestClient.publish(this.topicQualifiedName, this.body);

        return response;
    }

    public WebTestClient.ResponseSpec getResponse() {
        return this.response;
    }
}
