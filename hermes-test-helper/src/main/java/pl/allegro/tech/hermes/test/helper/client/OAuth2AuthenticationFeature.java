package pl.allegro.tech.hermes.test.helper.client;


import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;

public class OAuth2AuthenticationFeature implements ClientRequestFilter {

    private final String authToken;

    public OAuth2AuthenticationFeature(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add("Authorization", String.format("Token %s", authToken));
    }
}
