package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import pl.allegro.tech.hermes.api.AuthenticationType;
import pl.allegro.tech.hermes.api.Subscription;

import java.util.Optional;

public class HttpAuthorizationProviderFactory {

    // this factory should accept full Subscription object in the future
    // if we want to add more sophisticated authorization methods
    // like OAuth - this data will be most likely held in Subscription object
    public Optional<HttpAuthorizationProvider> create(Subscription subscription) {
        if(subscription.getAuthenticationType() == AuthenticationType.BASIC) {
            return Optional.of(new BasicAuthProvider(subscription));
        } else if(subscription.getAuthenticationType() == AuthenticationType.OAUTH2) {
            return Optional.of(new OAuth2AuthProvider(subscription));
        }
        else {
            return Optional.empty();
        }
    }

}
