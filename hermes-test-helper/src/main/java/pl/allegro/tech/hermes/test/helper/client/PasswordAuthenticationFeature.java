package pl.allegro.tech.hermes.test.helper.client;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;

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
