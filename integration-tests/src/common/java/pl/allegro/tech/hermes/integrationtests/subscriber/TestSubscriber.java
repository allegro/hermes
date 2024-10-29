package pl.allegro.tech.hermes.integrationtests.subscriber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Streams;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TestSubscriber {

  private static final int DEFAULT_WAIT_TIME_IN_SEC = 10;
  private final URI subscriberUrl;
  private final List<LoggedRequest> receivedRequests =
      Collections.synchronizedList(new ArrayList<>());

  TestSubscriber(URI subscriberUrl) {
    this.subscriberUrl = subscriberUrl;
  }

  void onRequestReceived(LoggedRequest request) {
    receivedRequests.add(request);
  }

  public String getEndpoint() {
    return subscriberUrl.toString();
  }

  public int getPort() {
    return subscriberUrl.getPort();
  }

  public String getPath() {
    return subscriberUrl.getPath();
  }

  public void waitUntilReceived(String body) {
    awaitWithSyncRequests(
        () ->
            assertThat(
                    receivedRequests.stream()
                        .filter(r -> r.getBodyAsString().equals(body))
                        .findFirst())
                .isNotEmpty());
  }

  public void waitUntilReceived(Duration duration, int numberOfExpectedMessages) {
    await()
        .atMost(adjust(duration))
        .untilAsserted(
            () -> assertThat(receivedRequests.size()).isEqualTo(numberOfExpectedMessages));
  }

  public void waitUntilAnyMessageReceived() {
    await()
        .atMost(adjust(Duration.ofSeconds(DEFAULT_WAIT_TIME_IN_SEC)))
        .untilAsserted(() -> assertThat(receivedRequests.size()).isPositive());
  }

  public void waitUntilRequestReceived(Consumer<LoggedRequest> requestConsumer) {
    waitUntilAnyMessageReceived();

    synchronized (receivedRequests) {
      receivedRequests.forEach(requestConsumer);
    }
  }

  public void waitUntilRequestsReceived(Consumer<List<LoggedRequest>> requestsConsumer) {
    await()
        .atMost(adjust(Duration.ofSeconds(DEFAULT_WAIT_TIME_IN_SEC)))
        .untilAsserted(
            () -> {
              synchronized (receivedRequests) {
                requestsConsumer.accept(receivedRequests);
              }
            });
  }

  public void noMessagesReceived() {
    assertThat(receivedRequests).isEmpty();
  }

  public void waitUntilMessageWithHeaderReceived(String headerName, String headerValue) {
    awaitWithSyncRequests(
        () ->
            assertThat(
                    receivedRequests.stream()
                        .filter(
                            r ->
                                r.containsHeader(headerName)
                                    && r.getHeader(headerName).equals(headerValue))
                        .findFirst())
                .isNotEmpty());
  }

  public java.time.Duration durationBetweenFirstAndLastRequest() {
    return java.time.Duration.between(
        getFirstReceivedRequest().getLoggedDate().toInstant(),
        getLastReceivedRequest().getLoggedDate().toInstant());
  }

  private void awaitWithSyncRequests(Runnable runnable) {
    await()
        .atMost(adjust(Duration.ofSeconds(DEFAULT_WAIT_TIME_IN_SEC)))
        .untilAsserted(
            () -> {
              synchronized (receivedRequests) {
                runnable.run();
              }
            });
  }

  private LoggedRequest getFirstReceivedRequest() {
    synchronized (receivedRequests) {
      return receivedRequests.stream().findFirst().orElseThrow(NoSuchElementException::new);
    }
  }

  public LoggedRequest getLastReceivedRequest() {
    synchronized (receivedRequests) {
      return Streams.findLast(receivedRequests.stream()).orElseThrow(NoSuchElementException::new);
    }
  }

  public void waitUntilAllReceivedStrict(Set<String> expectedBodies) {
    awaitWithSyncRequests(
        () -> {
          int receivedCount = receivedRequests.size();
          assertThat(receivedCount).isEqualTo(expectedBodies.size());
          Set<String> actual =
              receivedRequests.stream()
                  .map(LoggedRequest::getBodyAsString)
                  .collect(Collectors.toSet());
          assertThat(actual).isEqualTo(expectedBodies);
        });
  }

  public void reset() {
    this.receivedRequests.clear();
  }
}
