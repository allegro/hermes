package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import pl.allegro.tech.hermes.api.AuthenticationType;
import pl.allegro.tech.hermes.api.DeliveryType;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.test.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;


public class BasicAuthProviderTest {

    @Test
    public void shouldReturnBasicAuthenticationToken() {
        HashMap<String,String> authData = new HashMap<String,String>();
        authData.put("username", "username");
        authData.put("password", "password");
        EndpointAddress endpointAddress = new EndpointAddress("http://localhost:8080");
        Subscription subscription = Subscription.create(null, "name", endpointAddress, AuthenticationType.BASIC, authData, null, null, null, true, 
                                                      null, null, null, null, DeliveryType.SERIAL, null);
        BasicAuthProvider provider = new BasicAuthProvider(subscription);
        Message message = MessageBuilder.withTestMessage().build();

        String credentials = "username:password";
        String expectedToken = "Basic " + Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
        assertThat(provider.authorizationToken(message)).isEqualTo(expectedToken);
    }
}