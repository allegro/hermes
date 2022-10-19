package pl.allegro.tech.hermes.test.helper.client;

import java.io.IOException;
import java.util.function.Function;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

public class OAuth2AuthenticationFeature implements ClientRequestFilter {

    private final Function<ClientRequestContext, String> authTokenSupplier;

    public OAuth2AuthenticationFeature(Function<ClientRequestContext, String> authTokenSupplier) {
        this.authTokenSupplier = authTokenSupplier;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add("Authorization", String.format("Token %s", authTokenSupplier.apply(requestContext)));
    }
}
