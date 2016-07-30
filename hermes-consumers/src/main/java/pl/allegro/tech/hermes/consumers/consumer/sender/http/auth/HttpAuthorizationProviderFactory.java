package pl.allegro.tech.hermes.consumers.consumer.sender.http.auth;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthAccessTokens;

import javax.inject.Inject;
import java.util.Optional;

public class HttpAuthorizationProviderFactory {

    private final OAuthAccessTokens accessTokens;

    @Inject
    public HttpAuthorizationProviderFactory(OAuthAccessTokens accessTokens) {
        this.accessTokens = accessTokens;
    }

    public Optional<HttpAuthorizationProvider> create(Subscription subscription) {
        if(subscription.getEndpoint().containsCredentials()) {
            return Optional.of(new BasicAuthProvider(subscription.getEndpoint()));
        } else if (subscription.hasOAuthPolicy()) {
            return Optional.of(new OAuthHttpAuthorizationProvider(subscription.getQualifiedName(), accessTokens));
        }
        return Optional.empty();
    }

}
