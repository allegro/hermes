package pl.allegro.tech.hermes.consumers.consumer.oauth.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.entity.ContentType;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthAccessToken;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OAuthHttpClient implements OAuthClient {

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    @Inject
    public OAuthHttpClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public OAuthAccessToken getToken(OAuthTokenRequest request) {
        ContentResponse response = performHttpRequest(request);
        validateHttpResponse(response);
        OAuthTokenResponse accessTokenResponse = httpResponseToOAuthAccessTokenResponse(response);

        return accessTokenResponse.toAccessToken();
    }

    private Request createHttpRequest(OAuthTokenRequest request) {
        Request httpRequest = httpClient.newRequest(request.getUrl())
                .timeout(request.getRequestTimeout(), TimeUnit.MILLISECONDS)
                .method(HttpMethod.POST)
                .header(HttpHeader.KEEP_ALIVE, "true")
                .header(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.toString())
                .param(OAuthTokenRequest.Param.GRANT_TYPE, request.getGrantType())
                .param(OAuthTokenRequest.Param.SCOPE, request.getScope())
                .param(OAuthTokenRequest.Param.CLIENT_ID, request.getClientId())
                .param(OAuthTokenRequest.Param.CLIENT_SECRET, request.getClientSecret());

        if (OAuthTokenRequest.GrantTypeValue.RESOURCE_OWNER_USERNAME_PASSWORD.equals(request.getGrantType())) {
                httpRequest
                        .param(OAuthTokenRequest.Param.USERNAME, request.getUsername())
                        .param(OAuthTokenRequest.Param.PASSWORD, request.getPassword());
        }
        return httpRequest;
    }

    private ContentResponse performHttpRequest(OAuthTokenRequest request) {
        try {
            return createHttpRequest(request).send();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new OAuthTokenRequestException("An exception occurred while performing token request", e);
        }
    }

    private void validateHttpResponse(ContentResponse response) {
        if (response.getStatus() != HttpStatus.OK_200) {
            throw new OAuthTokenRequestException(String.format("%d %s response when performing token request",
                    response.getStatus(), response.getContentAsString()));
        }
    }

    private OAuthTokenResponse httpResponseToOAuthAccessTokenResponse(ContentResponse response) {
        try {
            return objectMapper.readValue(response.getContentAsString(), OAuthTokenResponse.class);
        } catch (IOException e) {
            throw new OAuthTokenRequestException("An exception occurred while reading token response", e);
        }
    }
}
