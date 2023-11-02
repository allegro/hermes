package pl.allegro.tech.hermes.integrationtests.client;

import org.springframework.test.web.reactive.server.WebTestClient;

class FrontendTestClient {
    private final WebTestClient webTestClient;

    public FrontendTestClient(String frontendContainerUrl) {
        this.webTestClient = WebTestClient
                .bindToServer()
                .baseUrl(frontendContainerUrl)
                .build();
    }

}
