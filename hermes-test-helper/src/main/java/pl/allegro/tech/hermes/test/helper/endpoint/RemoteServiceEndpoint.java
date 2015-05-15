package pl.allegro.tech.hermes.test.helper.endpoint;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.jayway.awaitility.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

public class RemoteServiceEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(RemoteServiceEndpoint.class);

    private final List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());

    private int expectedMessagesCount = 0;

    private final String path;

    private final WireMock listener;

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
                    receivedMessages.add(LoggedRequest.createFrom(request).getBodyAsString());
                }
            }
        });
    }

    public void expectMessages(TestMessage... messages) {
        expectMessages(Lists.transform(Arrays.asList(messages), new Function<TestMessage, String>() {
            @Override
            public String apply(TestMessage input) {
                return input.body();
            }
        }));
    }

    public void expectMessages(String... messages) {
        expectMessages(Arrays.asList(messages));
    }

    public void expectMessages(List<String> messages) {
        receivedMessages.clear();
        for (String message : messages) {
            listener.register(post(urlEqualTo(path))
                            .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(message)
                            )
            );
        }
        expectedMessagesCount = messages.size();
    }

    public void waitUntilReceived(long seconds) {
        logger.info("Expecting to receive {} messages", expectedMessagesCount);
        await().atMost(new Duration(seconds, TimeUnit.SECONDS)).until(() -> receivedMessages.size() == expectedMessagesCount);
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
        assertThat(receivedMessages).isEmpty();
    }
}
