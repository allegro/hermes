package pl.allegro.tech.hermes.integrationtests.subscriber;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static jakarta.ws.rs.core.Response.Status.OK;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TestSubscribersExtension implements AfterEachCallback, AfterAllCallback {

  private final WireMockServer service;
  private final URI serviceUrl;
  private final AtomicInteger subscriberIndex = new AtomicInteger();
  private final Map<String, TestSubscriber> subscribersPerPath = new ConcurrentHashMap<>();

  public TestSubscribersExtension() {
    this(0);
  }

  public TestSubscribersExtension(int port) {
    service = new WireMockServer(port);
    service.start();
    serviceUrl = URI.create("http://localhost:" + service.port());
    service.addMockServiceRequestListener(
        (request, response) -> {
          TestSubscriber subscriber =
              subscribersPerPath.get(request.getUrl()); // getUrl() returns path here
          if (subscriber != null) {
            subscriber.onRequestReceived(LoggedRequest.createFrom(request));
          }
        });
  }

  public TestSubscriber createSubscriber(String endpointPathSuffix) {
    return createSubscriber(OK.getStatusCode(), endpointPathSuffix);
  }

  public TestSubscriber createSubscriber() {
    return createSubscriber("");
  }

  public TestSubscriber createSubscriber(int statusCode) {
    return createSubscriber(statusCode, "");
  }

  public TestSubscriber createSubscriber(int statusCode, String endpointPathSuffix) {
    String path = createPath(endpointPathSuffix);
    return createSubscriberWithStrictPath(statusCode, path);
  }

  public TestSubscriber createSubscriberWithStrictPath(int statusCode, String path) {
    service.addStubMapping(
        post(urlPathEqualTo(path)).willReturn(aResponse().withStatus(statusCode)).build());
    TestSubscriber subscriber = new TestSubscriber(createSubscriberURI(path));
    subscribersPerPath.put(path, subscriber);
    return subscriber;
  }

  public TestSubscriber createSubscriberWithRetry(String message, int delay) {
    String path = createPath("");
    int firstStatusCode = 503;
    int secondStatusCode = 200;
    String scenarioName = "Retrying";
    String secondScenarioState = "Retried";
    service.addStubMapping(
        post(urlEqualTo(path))
            .withRequestBody(new EqualToPattern(message))
            .inScenario(scenarioName)
            .whenScenarioStateIs(STARTED)
            .willSetStateTo(secondScenarioState)
            .willReturn(
                aResponse()
                    .withStatus(firstStatusCode)
                    .withHeader("Retry-After", Integer.toString(delay))
                    .withFixedDelay(delay))
            .build());
    service.addStubMapping(
        post(urlEqualTo(path))
            .withRequestBody(new EqualToPattern(message))
            .inScenario(scenarioName)
            .whenScenarioStateIs(secondScenarioState)
            .willReturn(aResponse().withStatus(secondStatusCode).withFixedDelay(delay))
            .build());

    TestSubscriber subscriber = new TestSubscriber(createSubscriberURI(path));
    subscribersPerPath.put(path, subscriber);
    return subscriber;
  }

  private String createPath(String pathSuffix) {
    return "/subscriber-" + subscriberIndex.incrementAndGet() + pathSuffix;
  }

  public interface SubscriberScenario {
    void apply(WireMockServer subscriber, String endpoint);
  }

  public TestSubscriber createSubscriber(SubscriberScenario scenario) {
    String path = createPath("");
    scenario.apply(service, path);
    TestSubscriber subscriber = new TestSubscriber(createSubscriberURI(path));
    subscribersPerPath.put(path, subscriber);
    return subscriber;
  }

  private URI createSubscriberURI(String path) {
    return serviceUrl.resolve(path);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    service.resetRequests();
    subscribersPerPath.clear();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    service.stop();
  }

  public int getPort() {
    return service.port();
  }
}
