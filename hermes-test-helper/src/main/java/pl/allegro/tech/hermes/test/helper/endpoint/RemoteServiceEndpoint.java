package pl.allegro.tech.hermes.test.helper.endpoint;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Iterables;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.net.URI;
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
import static java.util.stream.Collectors.toList;
import static jakarta.ws.rs.core.Response.Status.MOVED_PERMANENTLY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class RemoteServiceEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(RemoteServiceEndpoint.class);

    private final List<LoggedRequest> receivedRequests = Collections.synchronizedList(new ArrayList<>());
    private final String path;
    private final URI url;

    private final WireMock listener;
    private final WireMockServer service;

    private List<String> expectedMessages = new ArrayList<>();

    private int returnedStatusCode = 200;
    private int delay = 0;

    public RemoteServiceEndpoint(WireMockServer service) {
        this(service, "/");
    }

    public RemoteServiceEndpoint(WireMockServer service, final String path) {
        this.listener = new WireMock("localhost", service.port());
        this.path = path;
        this.url = URI.create(String.format("http://localhost:%d%s", service.port(), path));
        this.service = service;

        service.resetMappings();
        service.resetRequests();
        service.resetScenarios();

        service.addMockServiceRequestListener((request, response) -> {
            if (path.equals(request.getUrl())) {
                receivedRequests.add(LoggedRequest.createFrom(request));
            }
        });
    }

    public void expectMessages(TestMessage... messages) {
        expectMessages(Arrays.stream(messages).map(TestMessage::body).collect(toList()));
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
                                .willReturn(aResponse().withStatus(returnedStatusCode).withFixedDelay(delay))));
    }

    public void redirectMessage(String message) {
        receivedRequests.clear();

        expectedMessages = Collections.singletonList(message);

        listener.register(
                post(urlEqualTo(path))
                        .willReturn(aResponse()
                                .withStatus(MOVED_PERMANENTLY.getStatusCode())
                                .withHeader("Location", "http://localhost:" + service.port())));
    }

    public void retryMessage(String message, int delay) {
        receivedRequests.clear();
        expectedMessages = Arrays.asList(message, message);
        int retryStatusCode = 503;
        listener.register(
                post(urlEqualTo(path))
                        .inScenario("retrying")
                        .whenScenarioStateIs(STARTED)
                        .willSetStateTo("retried")
                        .willReturn(aResponse()
                                .withStatus(retryStatusCode)
                                .withHeader("Retry-After", Integer.toString(delay))
                                .withFixedDelay(delay)));
        listener.register(
                post(urlEqualTo(path))
                        .inScenario("retrying")
                        .whenScenarioStateIs("retried")
                        .willReturn(aResponse().withStatus(returnedStatusCode).withFixedDelay(delay)));
    }

    public void slowThenFastMessage(String message, int chunks, int delayMs) {
        receivedRequests.clear();
        expectedMessages = Arrays.asList(message, message);

        listener.register(
                post(urlEqualTo(path))
                        .inScenario("slowAndFast")
                        .whenScenarioStateIs(STARTED)
                        .willSetStateTo("slow")
                        .willReturn(
                                aResponse()
                                        .withStatus(returnedStatusCode)
                                        .withBody("I am very slow!")
                                        .withChunkedDribbleDelay(chunks, delayMs)
                        )
        );

        listener.register(
                post(urlEqualTo(path))
                        .inScenario("slowAndFast")
                        .whenScenarioStateIs("slow")
                        .willReturn(
                                aResponse()
                                        .withStatus(returnedStatusCode)
                                        .withFixedDelay(0)
                        )
        );
    }

    public void setReturnedStatusCode(int statusCode) {
        returnedStatusCode = statusCode;
    }

    public void waitUntilReceived(long seconds) {
        logger.info("Expecting to receive {} messages", expectedMessages.size());
        await().atMost(adjust(Duration.ofSeconds(seconds))).untilAsserted(() ->
                assertThat(receivedRequests.size()).isGreaterThanOrEqualTo(expectedMessages.size()));
        synchronized (receivedRequests) {
            assertThat(receivedRequests.stream().map(LoggedRequest::getBodyAsString).collect(toList())).containsAll(expectedMessages);
        }
    }

    public void waitUntilReceived(long seconds, int numberOfExpectedMessages) {
        waitUntilReceived(seconds, numberOfExpectedMessages, body -> {
        });
    }

    public void waitUntilReceived(Consumer<String> requestBodyConsumer) {
        waitUntilRequestReceived(loggedRequest -> requestBodyConsumer.accept(loggedRequest.getBodyAsString()));
    }

    public void waitUntilRequestReceived(Consumer<LoggedRequest> requestConsumer) {
        waitUntilReceived(60, 1, requestConsumer);
    }

    public void waitUntilReceived(long seconds, int numberOfExpectedMessages, Consumer<LoggedRequest> requestBodyConsumer) {
        logger.info("Expecting to receive {} messages", numberOfExpectedMessages);
        await().atMost(adjust(Duration.ofSeconds(seconds))).untilAsserted(() ->
                assertThat(receivedRequests.size()).isGreaterThanOrEqualTo(numberOfExpectedMessages));
        synchronized (receivedRequests) {
            receivedRequests.forEach(requestBodyConsumer);
        }
    }

    public void waitUntilReceived(Duration duration, int numberOfExpectedMessages, Consumer<LoggedRequest> requestBodyConsumer) {
        logger.info("Expecting to receive {} messages", numberOfExpectedMessages);
        await().atMost(duration).untilAsserted(() -> assertThat(receivedRequests.size()).isEqualTo(numberOfExpectedMessages));
        synchronized (receivedRequests) {
            receivedRequests.forEach(requestBodyConsumer);
        }
    }

    public void waitUntilReceived() {
        this.waitUntilReceived(60);
    }

    public void makeSureNoneReceived() {
        logger.info("Expecting to receive no messages");
        assertThat(receivedRequests).isEmpty();
    }

    public LoggedRequest getLastReceivedRequest() {
        synchronized (receivedRequests) {
            return Iterables.getLast(receivedRequests);
        }
    }

    public LoggedRequest getFirstReceivedRequest() {
        LoggedRequest item = Iterables.getFirst(receivedRequests, null);
        if (item == null) {
            throw new NoSuchElementException();
        }
        return item;
    }

    public boolean receivedMessageWithHeader(String header, String value) {
        synchronized (receivedRequests) {
            return receivedRequests.stream().anyMatch(r -> r.header(header).containsValue(value));
        }
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
        delay = 0;
        returnedStatusCode = 200;
        receivedRequests.clear();
        listener.resetMappings();
        service.resetMappings();
    }

    public RemoteServiceEndpoint setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    public URI getUrl() {
        return url;
    }

    public int getServicePort() {
        return this.service.port();
    }

    public void stop() {
        listener.shutdown();
        service.shutdown();
    }

}
