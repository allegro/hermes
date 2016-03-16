package pl.allegro.tech.hermes.test.helper.endpoint;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Iterables;
import com.jayway.awaitility.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class RemoteServiceEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(RemoteServiceEndpoint.class);

    private final List<LoggedRequest> receivedRequests = Collections.synchronizedList(new ArrayList<>());
    private final String path;

    private final WireMock listener;

    private List<String> expectedMessages = new ArrayList<>();

    private int returnedStatusCode = 200;
    private int retryStatusCode = 503;

    public RemoteServiceEndpoint(WireMockServer service) {
        this(service, "/");
    }

    public RemoteServiceEndpoint(WireMockServer service, final String path) {
        this.listener = new WireMock("localhost", service.port());
        this.path = path;
        service.addMockServiceRequestListener(new RequestListener() {
            @Override
            public void requestReceived(Request request, Response response) {
                if (path.equals(request.getUrl())) {
                    receivedRequests.add(LoggedRequest.createFrom(request));
                }
            }
        });
    }

    public void expectMessages(TestMessage... messages) {
        expectMessages(Arrays.asList(messages).stream().map(TestMessage::body).collect(toList()));
    }

    public void expectMessages(String... messages) {
        expectMessages(Arrays.asList(messages));
    }

    public void expectMessages(List<String> messages) {
        receivedRequests.clear();
        expectedMessages = messages;
        messages.forEach(m -> listener
            .register(
                post(urlEqualTo(path))
                .willReturn(aResponse().withStatus(returnedStatusCode))));
    }

    public void retryMessage(String message, int delay) {
        receivedRequests.clear();
        expectedMessages = Arrays.asList(message, message);
        listener.register(
                post(urlEqualTo(path))
                        .inScenario("retrying")
                        .whenScenarioStateIs(STARTED)
                        .willSetStateTo("retried")
                        .willReturn(aResponse()
                                .withStatus(retryStatusCode)
                                .withHeader("Retry-After", Integer.toString(delay))));
        listener.register(
                post(urlEqualTo(path))
                        .inScenario("retrying")
                        .whenScenarioStateIs("retried")
                        .willReturn(aResponse().withStatus(returnedStatusCode)));
    }

    public void setReturnedStatusCode(int statusCode) {
        returnedStatusCode = statusCode;
    }

    public void waitUntilReceived(long seconds) {
        logger.info("Expecting to receive {} messages", expectedMessages.size());
        await().atMost(adjust(new Duration(seconds, TimeUnit.SECONDS))).until(() -> receivedRequests.size() == expectedMessages.size());
        assertThat(receivedRequests.stream().map(LoggedRequest::getBodyAsString).collect(toList())).containsAll(expectedMessages);
    }

    public void waitUntilReceived(long seconds, int numberOfExpectedMessages) {
        waitUntilReceived(seconds, numberOfExpectedMessages, body -> {});
    }

    public void waitUntilReceived(Consumer<String> requestBodyConsumer) {
        waitUntilRequestReceived(loggedRequest -> requestBodyConsumer.accept(loggedRequest.getBodyAsString()));
    }

    public void waitUntilRequestReceived(Consumer<LoggedRequest> requestConsumer) {
        waitUntilReceived(60, 1, requestConsumer);
    }

    public void waitUntilReceived(long seconds, int numberOfExpectedMessages, Consumer<LoggedRequest> requestBodyConsumer) {
        logger.info("Expecting to receive {} messages", numberOfExpectedMessages);
        await().atMost(adjust(new Duration(seconds, TimeUnit.SECONDS))).until(() -> receivedRequests.size() == numberOfExpectedMessages);
        receivedRequests.stream().forEach(requestBodyConsumer::accept);
    }

    public void waitUntilReceived(Duration duration, int numberOfExpectedMessages, Consumer<LoggedRequest> requestBodyConsumer) {
        logger.info("Expecting to receive {} messages", numberOfExpectedMessages);
        await().atMost(duration).until(() -> receivedRequests.size() == numberOfExpectedMessages);
        receivedRequests.stream().forEach(requestBodyConsumer::accept);
    }

    public void waitUntilReceived() {
        this.waitUntilReceived(60);
    }

    public void makeSureNoneReceived() {
        logger.info("Expecting to receive no messages");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException exception) {
            logger.info("Who interrupted me?!", exception);
        }
        assertThat(receivedRequests).isEmpty();
    }

    public LoggedRequest getLastReceivedRequest() {
        return Iterables.getLast(receivedRequests);
    }

    public LoggedRequest getFirstReceivedRequest() {
        LoggedRequest item = Iterables.getFirst(receivedRequests, null);
        if (item == null) {
            throw new NoSuchElementException();
        }
        return item;
    }

    public boolean receivedMessageWithHeader(String header, String value) {
        return receivedRequests.stream().anyMatch(r -> r.header(header).containsValue(value));
    }

    public java.time.Duration durationBetweenFirstAndLastRequest() {
        return java.time.Duration.between(
                getFirstReceivedRequest().getLoggedDate().toInstant(),
                getLastReceivedRequest().getLoggedDate().toInstant());
    }

    public LoggedRequest waitAndGetLastRequest() {
        waitUntilReceived();
        return getLastReceivedRequest();
    }

    public void reset() {
        receivedRequests.clear();
    }
}
