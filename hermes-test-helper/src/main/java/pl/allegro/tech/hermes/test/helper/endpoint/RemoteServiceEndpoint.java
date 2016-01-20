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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
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

    public void setReturnedStatusCode(int statusCode) {
        returnedStatusCode = statusCode;
    }

    public void waitUntilReceived(long seconds) {
        logger.info("Expecting to receive {} messages", expectedMessages.size());
        await().atMost(adjust(new Duration(seconds, TimeUnit.SECONDS))).until(() -> receivedRequests.size() == expectedMessages.size());
        assertThat(receivedRequests.stream().map(LoggedRequest::getBodyAsString).collect(toList())).containsAll(expectedMessages);
    }

    public void waitUntilReceived(Consumer<String> requestBodyConsumer) {
        waitUntilReceived(60, 1, requestBodyConsumer);
    }

    public void waitUntilReceived(long seconds, int numberOfExpectedMessages, Consumer<String> requestBodyConsumer) {
        logger.info("Expecting to receive {} messages", numberOfExpectedMessages);
        await().atMost(adjust(new Duration(seconds, TimeUnit.SECONDS))).until(() -> receivedRequests.size() == numberOfExpectedMessages);
        receivedRequests.stream().map(LoggedRequest::getBodyAsString).forEach(requestBodyConsumer::accept);
    }

    public void waitUntilReceived(Duration duration, int numberOfExpectedMessages, Consumer<String> requestBodyConsumer) {
        logger.info("Expecting to receive {} messages", numberOfExpectedMessages);
        await().atMost(duration).until(() -> receivedRequests.size() == numberOfExpectedMessages);
        receivedRequests.stream().map(request -> request.getBodyAsString()).forEach(requestBodyConsumer::accept);
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

    public LoggedRequest waitAndGetLastRequest() {
        waitUntilReceived();
        return getLastReceivedRequest();
    }

    public void reset() {
        receivedRequests.clear();
    }
}
