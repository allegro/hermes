package pl.allegro.tech.hermes.integrationtests.setup;

import com.github.tomakehurst.wiremock.WireMockServer;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

public class CrowdExtension implements BeforeAllCallback, AfterEachCallback, AfterAllCallback {

    private static final String BASE_API_PATH = "/crowd/rest/usermanagement/1/search";

    private final WireMockServer wiremock = new WireMockServer(0);

    @Override
    public void beforeAll(ExtensionContext context) {
        wiremock.start();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        wiremock.resetAll();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        wiremock.stop();
    }

    public String getEndpoint() {
        return "http://localhost:" + wiremock.port() + "/crowd";
    }

    public void stubGroups(String ... groups) {
        var body = new StringBuilder("{ \"expand\": \"group\", \"groups\": [");
        for (int i = 0; i < groups.length; i++) {
            body.append(groupResponse("http://main/crowd/groupname/" + i, groups[i]));
            if (i < groups.length - 1)
                body.append(",");
        }
        body.append("]}");
        wiremock.addStubMapping(
                get(urlMatching(BASE_API_PATH + ".*"))
                        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                        .withBody(body.toString())
                        )
                        .build()
        );
    }

    public void assertRequestCount(int count) {
        wiremock.verify(count, getRequestedFor(urlMatching(BASE_API_PATH + ".*")));
    }

    private String groupResponse(String path, String name) {
        return String.format("{ \"link\": { \"href\": \"%s\", \"rel\": \"self\" }, \"name\": \"%s\"}", path, name);
    }

    public void stubDelay(Duration duration) {
        wiremock.addStubMapping(
                get(urlMatching(BASE_API_PATH + ".*"))
                        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                        .withBody("{ \"expand\": \"group\", \"groups\": []}")
                                        .withFixedDelay((int) duration.toMillis())
                        )
                        .build()
        );
    }
}
