package pl.allegro.tech.hermes.integrationtests.client;

import org.springframework.test.web.reactive.server.WebTestClient;

final class PublisherCallable {

    private WebTestClient.ResponseSpec response;

    private final HermesTestClient hermesTestClient;

    private final String topicQualifiedName;

    private final String body;

    public PublisherCallable(HermesTestClient hermesTestClient, String topicQualifiedName, String body) {
        this.hermesTestClient = hermesTestClient;
        this.topicQualifiedName = topicQualifiedName;
        this.body = body;
    }

    public WebTestClient.ResponseSpec call() {
        this.response = hermesTestClient.publishResponse(this.topicQualifiedName, this.body);

        return response;
    }

    public WebTestClient.ResponseSpec getResponse() {
        return this.response;
    }
}
