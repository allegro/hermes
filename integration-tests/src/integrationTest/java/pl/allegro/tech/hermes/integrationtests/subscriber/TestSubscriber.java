package pl.allegro.tech.hermes.integrationtests.subscriber;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.awaitility.Duration;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.jayway.awaitility.Awaitility.await;
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

    public void waitUntilReceived(String body) {
        waitUntilAnyMessageReceived();
        checkMessageReceived(body);
    }

    private void checkMessageReceived(String body) {
        assertThat(receivedRequests.stream()
            .map(LoggedRequest::getBodyAsString)
            .filter(bodyAsString -> bodyAsString.equals(body))
            .findAny()).isNotEmpty();
    }

    public void waitUntilAnyMessageReceived() {
        await().atMost(adjust(new Duration(DEFAULT_WAIT_TIME_IN_SEC, TimeUnit.SECONDS))).until(() ->
            assertThat(receivedRequests.size()).isPositive());
    }

    public void waitUntilRequestReceived(Consumer<LoggedRequest> requestConsumer) {
        waitUntilAnyMessageReceived();
        receivedRequests.forEach(requestConsumer);
    }

    public void noMessagesReceived() {
        assertThat(receivedRequests).isEmpty();
    }
}
