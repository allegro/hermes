package pl.allegro.tech.hermes.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public class HermesMock extends WireMockServer implements MethodRule, TestRule {
    private Map<String, Integer> topics;

    public HermesMock(int port) {
        super(port);
    }

    public void addTopic(String topicName) {
        addTopic(topicName, aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("\"testing-library\": \"WireMock\"")
        );
    }

    public void addTopic(String topicName, ResponseDefinitionBuilder responseDefinitionBuilder) {
        stubFor(get(urlPathMatching("/topics/" + topicName))
                .willReturn(responseDefinitionBuilder)
        );
    }

    public void assertTopic(String topicName) {
        expectTopic(1, topicName);
    }

    public void expectTopic(int count, String topicName) {
        verify(count, postRequestedFor(urlEqualTo("/topics/" + topicName)));
    }

    public List<LoggedRequest> getAllRequests(String topicName) {
        return findAll(postRequestedFor(urlEqualTo("/topics/" + topicName)));
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                start();
                base.evaluate();
                stop();
            }
        };
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return apply(base, null, null);
    }
}

