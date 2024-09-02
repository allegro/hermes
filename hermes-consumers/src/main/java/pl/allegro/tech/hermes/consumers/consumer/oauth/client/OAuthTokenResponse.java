package pl.allegro.tech.hermes.consumers.consumer.oauth.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthAccessToken;

public class OAuthTokenResponse {

  @NotEmpty private final String accessToken;

  private final Integer expiresIn;

  @JsonCreator
  public OAuthTokenResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("expires_in") Integer expiresIn) {
    this.accessToken = accessToken;
    this.expiresIn = expiresIn;
  }

  public OAuthAccessToken toAccessToken() {
    return new OAuthAccessToken(accessToken, expiresIn);
  }
}
