package pl.allegro.tech.hermes.consumers.consumer.oauth.client;

public class OAuthTokenRequestException extends RuntimeException {

  public OAuthTokenRequestException(String message, Throwable cause) {
    super(message, cause);
  }

  public OAuthTokenRequestException(String message) {
    super(message);
  }
}
