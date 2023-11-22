package pl.allegro.tech.hermes.integrationtests.client;

import jakarta.ws.rs.core.UriBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;

class ConsumerTestClient {

    private static final String STATUS_SUBSCRIPTIONS = "/status/subscriptions";

    private final WebTestClient webTestClient;
    private final String consumerContainerUrl;

    public ConsumerTestClient(String consumerContainerUrl) {
        this.webTestClient = WebTestClient
                .bindToServer()
                .baseUrl(consumerContainerUrl)
                .build();
        this.consumerContainerUrl = consumerContainerUrl;
    }

    public WebTestClient.ResponseSpec getRunningSubscriptionsStatus() {
        return webTestClient.get().uri(UriBuilder.fromUri(consumerContainerUrl)
                        .path(STATUS_SUBSCRIPTIONS)
                        .build())
                .exchange();
    }
}
