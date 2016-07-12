package pl.allegro.tech.hermes.consumers.consumer.sender.http.auth;

import org.apache.commons.codec.binary.Base64;
import pl.allegro.tech.hermes.api.EndpointAddress;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class BasicAuthProvider implements HttpAuthorizationProvider {

    private final String token;

    public BasicAuthProvider(EndpointAddress endpoint) {
        String credentials = endpoint.getUsername() + ":" + endpoint.getPassword();
        String encodedCredentials = Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
        this.token = "Basic " + encodedCredentials;
    }

    @Override
    public Optional<String> authorizationToken() {
        return Optional.of(token);
    }
}
