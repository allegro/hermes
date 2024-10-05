package pl.allegro.tech.hermes.consumers.consumer.sender.http.auth;

import java.util.Optional;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthAccessToken;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthAccessTokens;

public class OAuthHttpAuthorizationProvider implements HttpAuthorizationProvider {

  private static final String BEARER_TOKEN_PREFIX = "Bearer ";

  private final SubscriptionName subscriptionName;

  private final OAuthAccessTokens accessTokens;

  public OAuthHttpAuthorizationProvider(
      SubscriptionName subscriptionName, OAuthAccessTokens accessTokens) {
    this.subscriptionName = subscriptionName;
    this.accessTokens = accessTokens;
  }

  @Override
  public Optional<String> authorizationToken() {
    return accessTokens
        .getTokenIfPresent(subscriptionName)
        .map(OAuthAccessToken::getTokenValue)
        .map(value -> BEARER_TOKEN_PREFIX + value);
  }
}
