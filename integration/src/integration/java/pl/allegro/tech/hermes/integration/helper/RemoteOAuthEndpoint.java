package pl.allegro.tech.hermes.integration.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.Response;
import com.google.common.collect.ImmutableMap;
import pl.allegro.tech.hermes.integration.env.EnvironmentAware;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.google.common.net.UrlEscapers.urlFormParameterEscaper;
import static java.lang.String.format;

public class RemoteOAuthEndpoint implements EnvironmentAware {

    public static final String AUTHORIZATION_PATH = "/authorization/ad";

    private final WireMock listener;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RemoteOAuthEndpoint(WireMockServer wireMockServer) {
        listener = new WireMock("localhost", wireMockServer.port());
        wireMockServer.addMockServiceRequestListener(new RequestListener() {
            @Override
            public void requestReceived(Request request, Response response) {
                System.out.println(request);
            }
        });
    }

    public void expectAdminAuthorizationAndAllow(String authToken, String rootGroupName) {
        expectAuthorization(authToken, rootGroupName, true);
    }

    public void expectAdminAuthorizationAndDeny(String authToken, String rootGroupName) {
        expectAuthorization(authToken, rootGroupName, false);
    }

    public void expectGroupAuthorizationAndAllow(String authToken, String groupName) {
        expectAuthorization(authToken, groupName, true);
    }

    public void expectGroupAuthorizationAndDeny(String authToken, String groupName) {
        expectAuthorization(authToken, groupName, false);
    }

    private void expectAuthorization(String authToken, String groupName, boolean result) {
        listener.resetMappings();
        listener.register(post(urlMatching(AUTHORIZATION_PATH))
            .withRequestBody(containing(format("resources=%s&token=%s", urlFormParameterEscaper().escape(groupName), authToken)))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(body(groupName, result))));
    }

    private String body(String rootGroupName, boolean result) {
        try {
            return objectMapper.writeValueAsString(ImmutableMap.of(rootGroupName, result));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

}
