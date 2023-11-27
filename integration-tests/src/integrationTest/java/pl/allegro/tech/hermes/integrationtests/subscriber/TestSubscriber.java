package pl.allegro.tech.hermes.integrationtests.subscriber;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Streams;
import com.jayway.awaitility.Duration;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class TestSubscriber {

    private static final int DEFAULT_WAIT_TIME_IN_SEC = 10;
    private final URI subscriberUrl;
    private final List<LoggedRequest> receivedRequests = Collections.synchronizedList(new ArrayList<>());

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
        awaitWithSyncRequests(() ->
            assertThat(
                receivedRequests.stream()
                .filter(r -> r.getBodyAsString().equals(body))
                .findFirst()
            ).isNotEmpty());
    }

    public void waitUntilAnyMessageReceived() {
        await().atMost(adjust(new Duration(DEFAULT_WAIT_TIME_IN_SEC, SECONDS))).until(() ->
            assertThat(receivedRequests.size()).isPositive());
    }

    public void waitUntilRequestReceived(Consumer<LoggedRequest> requestConsumer) {
        waitUntilAnyMessageReceived();

        synchronized (receivedRequests) {
            receivedRequests.forEach(requestConsumer);
        }
    }

    public void noMessagesReceived() {
        assertThat(receivedRequests).isEmpty();
    }

    public void waitUntilMessageWithHeaderReceived(String headerName, String headerValue) {
        awaitWithSyncRequests(() ->
            assertThat(
                receivedRequests.stream()
                .filter(r -> r.containsHeader(headerName) && r.getHeader(headerName).equals(headerValue))
                .findFirst())
            .isNotEmpty());
    }

    public java.time.Duration durationBetweenFirstAndLastRequest() {
        return java.time.Duration.between(
            getFirstReceivedRequest().getLoggedDate().toInstant(),
            getLastReceivedRequest().getLoggedDate().toInstant());
    }

    private void awaitWithSyncRequests(Runnable runnable) {
        await().atMost(adjust(new Duration(DEFAULT_WAIT_TIME_IN_SEC, SECONDS))).until(() -> {
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

    private LoggedRequest getLastReceivedRequest() {
        synchronized (receivedRequests) {
            return Streams.findLast(receivedRequests.stream()).orElseThrow(NoSuchElementException::new);
        }
    }

    public void reset() {
        this.receivedRequests.clear();
    }
}
