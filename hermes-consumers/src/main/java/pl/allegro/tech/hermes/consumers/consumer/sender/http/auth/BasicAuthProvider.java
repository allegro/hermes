package pl.allegro.tech.hermes.consumers.consumer.sender.http.auth;

import pl.allegro.tech.hermes.api.EndpointAddress;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public class BasicAuthProvider implements HttpAuthorizationProvider {

    private final String token;

    public BasicAuthProvider(EndpointAddress endpoint) {
        String credentials = endpoint.getUsername() + ":" + endpoint.getPassword();
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        this.token = "Basic " + encodedCredentials;
    }

    @Override
    public Optional<String> authorizationToken() {
        return Optional.of(token);
    }
}
