package pl.allegro.tech.hermes.test.helper.client.integration;

import jakarta.ws.rs.core.UriBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;

class ConsumerTestClient {

  private static final String STATUS_SUBSCRIPTIONS = "/status/subscriptions";

  private static final String METRICS_PATH = "/status/prometheus";

  private final WebTestClient webTestClient;
  private final String consumerContainerUrl;

  public ConsumerTestClient(int consumerPort) {
    this.consumerContainerUrl = "http://localhost:" + consumerPort;
    this.webTestClient =
        WebTestClient.bindToServer()
            .baseUrl(consumerContainerUrl)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
            .build();
  }

  public WebTestClient.ResponseSpec getRunningSubscriptionsStatus() {
    return webTestClient
        .get()
        .uri(UriBuilder.fromUri(consumerContainerUrl).path(STATUS_SUBSCRIPTIONS).build())
        .exchange();
  }

  public WebTestClient.ResponseSpec getMetrics() {
    return webTestClient
        .get()
        .uri(UriBuilder.fromUri(consumerContainerUrl).path(METRICS_PATH).build())
        .exchange();
  }
}
