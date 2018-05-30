package pl.allegro.tech.hermes.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

public class HermesMock implements MethodRule, TestRule {

    public enum RESPONSE_CODE {
        CREATED(201),
        ACCEPTED(202),
        BAD_MESSAGE(400),
        NOT_FOUND(404),
        REQUEST_TIMEOUT(408),
        INTERNAL_ERROR(500),
        SERVICE_UNAVAILABLE(503);

        private int code;

        RESPONSE_CODE(int code) {
            this.code = code;
        }
    }

    private WireMockServer wireMockServer;
    private ObjectMapper objectMapper = new ObjectMapper();

    public HermesMock(int port) {
        wireMockServer = new WireMockServer(port);
    }

    public void addTopic(String topicName) {
        addTopic(topicName, RESPONSE_CODE.CREATED);
    }

    public void addTopic(String topicName, RESPONSE_CODE statusCode) {
        addTopic(topicName, aResponse().withStatus(statusCode.code));
    }

    public void addTopic(String topicName, ResponseDefinitionBuilder responseDefinitionBuilder) {
        wireMockServer.stubFor(get(urlPathMatching("/topics/" + topicName))
                .willReturn(responseDefinitionBuilder
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Hermes-Message-Id", UUID.randomUUID().toString())
                )
        );
//                .withHeader("Content-Type", equalTo("avro/binary"))
    }

    public void assertTopic(String topicName) {
        expectTopic(1, topicName);
    }

    public void expectTopic(int count, String topicName) {
        try {
            await().atMost(5, SECONDS).until(() ->
                    wireMockServer.verify(count, postRequestedFor(urlEqualTo("/topics/" + topicName)))
            );
        } catch (Exception e) {
            throw new HermesMockException("");
        }
    }

    public List<LoggedRequest> getAllRequests() {
        return wireMockServer.findAll(postRequestedFor(urlMatching(("/topics/"))));
    }

    public List<LoggedRequest> getAllRequests(String topicName) {
        return wireMockServer.findAll(postRequestedFor(urlEqualTo("/topics/" + topicName)));
    }

    public <T> List<T> getAllMessagesAs(String topicName, Class<T> clazz) {
        final Function<LoggedRequest, T> deserialize = (req) -> {
            try {
                return objectMapper.readValue(req.getBodyAsString(), clazz);
            } catch (IOException ex) {
                return null;
            }
        };

        return getAllRequests(topicName).stream()
                .map(deserialize)
                .collect(toList());
    }

    public LoggedRequest getLastRequest(String topicName) {
        List<LoggedRequest> requests = getAllRequests(topicName);
        if (requests.isEmpty()) {
            return null;
        }
        return requests.get(0);
    }

    public <T> T getLastMessageAs(String topicName, Class<T> clazz) throws IOException {
        LoggedRequest request = getLastRequest(topicName);
        return objectMapper.readValue(request.getBodyAsString(), clazz);
    }

    public void resetReceivedRequest() {
        wireMockServer.resetRequests();
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                wireMockServer.start();
                base.evaluate();
                wireMockServer.stop();
            }
        };
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return apply(base, null, null);
    }
}
