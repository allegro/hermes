package pl.allegro.tech.hermes.consumers.consumer.sender.http.auth;

import java.util.Optional;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthAccessTokens;

public class HttpAuthorizationProviderFactory {

  private final OAuthAccessTokens accessTokens;

  public HttpAuthorizationProviderFactory(OAuthAccessTokens accessTokens) {
    this.accessTokens = accessTokens;
  }

  public Optional<HttpAuthorizationProvider> create(Subscription subscription) {
    if (subscription.getEndpoint().containsCredentials()) {
      return Optional.of(new BasicAuthProvider(subscription.getEndpoint()));
    } else if (subscription.hasOAuthPolicy()) {
      return Optional.of(
          new OAuthHttpAuthorizationProvider(subscription.getQualifiedName(), accessTokens));
    }
    return Optional.empty();
  }
}
