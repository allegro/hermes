package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.apache.commons.codec.binary.Base64;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.nio.charset.StandardCharsets;

public class BasicAuthProvider implements HttpAuthorizationProvider {

    private final String token;

    public BasicAuthProvider(Subscription subscription) {
        String credentials = subscription.getBasicAuthenticationData().getUsername() + ":" + 
                                subscription.getBasicAuthenticationData().getPassword();
        String encodedCredentials = Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
        this.token = "Basic " + encodedCredentials;
    }

    @Override
    public String authorizationToken(Message message) {
        return token;
    }
}
