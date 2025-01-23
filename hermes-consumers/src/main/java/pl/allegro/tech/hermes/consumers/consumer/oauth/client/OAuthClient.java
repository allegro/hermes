package pl.allegro.tech.hermes.consumers.consumer.oauth.client;

import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthAccessToken;

public interface OAuthClient {

  OAuthAccessToken getToken(OAuthTokenRequest request);

  void start();

  void stop();
}
