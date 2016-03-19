package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import pl.allegro.tech.hermes.api.EndpointAddress;

import java.util.Optional;

public class HttpAuthorizationProviderFactory {

    // this factory should accept full Subscription object in the future
    // if we want to add more sophisticated authorization methods
    // like OAuth - this data will be most likely held in Subscription object
    public Optional<HttpAuthorizationProvider> create(EndpointAddress endpoint) {
        if(endpoint.containsCredentials()) {
            return Optional.of(new BasicAuthProvider(endpoint));
        }
        else {
            return Optional.empty();
        }
    }

}
