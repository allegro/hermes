package pl.allegro.tech.hermes.test.helper.client;

import java.io.IOException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

public class PasswordAuthenticationFeature implements ClientRequestFilter {

    private final String password;

    public PasswordAuthenticationFeature(String password) {
        this.password = password;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add("Hermes-Admin-Password", password);
    }
}
