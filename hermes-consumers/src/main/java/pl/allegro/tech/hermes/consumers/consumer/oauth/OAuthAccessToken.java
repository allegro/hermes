package pl.allegro.tech.hermes.consumers.consumer.oauth;

import java.util.Objects;

public class OAuthAccessToken {

  private final String tokenValue;

  private final Integer expiresIn;

  public OAuthAccessToken(String tokenValue, Integer expiresIn) {
    this.tokenValue = tokenValue;
    this.expiresIn = expiresIn;
  }

  public String getTokenValue() {
    return tokenValue;
  }

  public Integer getExpiresIn() {
    return expiresIn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OAuthAccessToken that = (OAuthAccessToken) o;
    return Objects.equals(tokenValue, that.tokenValue) && Objects.equals(expiresIn, that.expiresIn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tokenValue, expiresIn);
  }
}
