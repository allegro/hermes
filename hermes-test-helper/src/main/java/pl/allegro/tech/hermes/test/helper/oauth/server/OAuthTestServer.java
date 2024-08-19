package pl.allegro.tech.hermes.test.helper.oauth.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;

public class OAuthTestServer {

    private static final String OAUTH2_TOKEN_ENDPOINT = "/oauth2/token";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WireMockServer wireMockServer;

    public OAuthTestServer() {
        wireMockServer = new WireMockServer(0);
    }

    public String getTokenEndpoint() {
        return String.format("http://localhost:%s%s", wireMockServer.port(), OAUTH2_TOKEN_ENDPOINT);
    }

    public String stubAccessTokenForPasswordGrant(OAuthClient client, OAuthResourceOwner resourceOwner) {
        String token = UUID.randomUUID().toString();
        Map<String, String> params = Map.of(
                "access_token", token,
                "token_type", "Bearer"
        );
        try {
            String body = objectMapper.writeValueAsString(params);
            wireMockServer.addStubMapping(
                    post(urlPathEqualTo(OAUTH2_TOKEN_ENDPOINT))
                            .withQueryParam("grant_type", equalTo("password"))
                            .withQueryParam("client_id", equalTo(client.clientId()))
                            .withQueryParam("client_secret", equalTo(client.secret()))
                            .withQueryParam("username", equalTo(resourceOwner.username()))
                            .withQueryParam("password", equalTo(resourceOwner.password()))
                            .willReturn(
                                    aResponse()
                                            .withBody(body)
                                            .withStatus(SC_OK)
                            )
                            .build());
            return token;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String stubAccessTokenForClientCredentials(OAuthClient client) {
        String token = UUID.randomUUID().toString();
        Map<String, String> params = Map.of(
                "access_token", token,
                "token_type", "Bearer"
        );
        try {
            String body = objectMapper.writeValueAsString(params);
            wireMockServer.addStubMapping(
                    post(urlPathEqualTo(OAUTH2_TOKEN_ENDPOINT))
                            .withQueryParam("grant_type", equalTo("client_credentials"))
                            .withQueryParam("client_id", equalTo(client.clientId()))
                            .withQueryParam("client_secret", equalTo(client.secret()))
                            .willReturn(
                                    aResponse()
                                            .withBody(body)
                                            .withStatus(SC_OK)
                            )
                            .build());
            return token;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void reset() {
        wireMockServer.resetAll();
    }

    public void start() {
        wireMockServer.start();
    }

    public void stop() {
        wireMockServer.stop();
    }
}
