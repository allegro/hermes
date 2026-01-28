package pl.allegro.tech.hermes.benchmark.environment;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.LongAdder;

public class CountingTestSubscriber {

  private final String path;
  private final WireMockServer service;
  private final LongAdder messageCount = new LongAdder();
  private final URI serviceUrl;

  public CountingTestSubscriber(String topicName) {
    service = new WireMockServer(0);
    service.start();
    serviceUrl = URI.create("http://localhost:" + service.port());
    path = "/test-subscriber/" + topicName;
    service.addStubMapping(
        post(urlPathEqualTo(path)).willReturn(aResponse().withStatus(200)).build());
    service.addMockServiceRequestListener(
        (request, response) -> {
          if (request.getUrl().equals(path)) {
            messageCount.increment();
          }
        });
  }

  public String getEndpoint() {
    return serviceUrl.resolve(path).toString();
  }

  public void waitUntilReceived(Duration duration, int numberOfExpectedMessages) {
    await()
        .atMost(adjust(duration))
        .untilAsserted(() -> assertThat(messageCount.sum()).isEqualTo(numberOfExpectedMessages));
  }

  public void stop() {
    service.stop();
  }
}
