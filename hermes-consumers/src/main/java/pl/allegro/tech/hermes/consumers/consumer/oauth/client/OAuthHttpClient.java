package pl.allegro.tech.hermes.consumers.consumer.oauth.client;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthAccessToken;

public class OAuthHttpClient implements OAuthClient {

  private final HttpClient httpClient;

  private final ObjectMapper objectMapper;

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
    Request httpRequest =
        httpClient
            .newRequest(request.getUrl())
            .timeout(request.getRequestTimeout(), TimeUnit.MILLISECONDS)
            .idleTimeout(request.getSocketTimeout(), TimeUnit.MILLISECONDS)
            .method(HttpMethod.POST)
            .headers(
                headers -> {
                  headers.add(HttpHeader.KEEP_ALIVE, "true");
                  headers.add(HttpHeader.CONTENT_TYPE, APPLICATION_FORM_URLENCODED);
                });
    addParamIfNotNull(httpRequest, OAuthTokenRequest.Param.GRANT_TYPE, request.getGrantType());
    addParamIfNotNull(httpRequest, OAuthTokenRequest.Param.SCOPE, request.getScope());
    addParamIfNotNull(httpRequest, OAuthTokenRequest.Param.CLIENT_ID, request.getClientId());
    addParamIfNotNull(
        httpRequest, OAuthTokenRequest.Param.CLIENT_SECRET, request.getClientSecret());

    if (OAuthTokenRequest.GrantTypeValue.RESOURCE_OWNER_USERNAME_PASSWORD.equals(
        request.getGrantType())) {
      addParamIfNotNull(httpRequest, OAuthTokenRequest.Param.USERNAME, request.getUsername());
      addParamIfNotNull(httpRequest, OAuthTokenRequest.Param.PASSWORD, request.getPassword());
    }
    return httpRequest;
  }

  private void addParamIfNotNull(Request request, String name, String value) {
    if (value != null) {
      request.param(name, value);
    }
  }

  private ContentResponse performHttpRequest(OAuthTokenRequest request) {
    try {
      return createHttpRequest(request).send();
    } catch (InterruptedException | TimeoutException | ExecutionException e) {
      throw new OAuthTokenRequestException(
          "An exception occurred while performing token request", e);
    }
  }

  private void validateHttpResponse(ContentResponse response) {
    if (response.getStatus() != HttpStatus.OK_200) {
      throw new OAuthTokenRequestException(
          String.format(
              "%d %s response when performing token request",
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

  @Override
  public void start() {
    try {
      this.httpClient.start();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void stop() {
    try {
      this.httpClient.stop();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
