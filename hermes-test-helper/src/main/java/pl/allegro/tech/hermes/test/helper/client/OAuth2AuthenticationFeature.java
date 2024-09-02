package pl.allegro.tech.hermes.test.helper.client;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.util.function.Function;

public class OAuth2AuthenticationFeature implements ClientRequestFilter {

  private final Function<ClientRequestContext, String> authTokenSupplier;

  public OAuth2AuthenticationFeature(Function<ClientRequestContext, String> authTokenSupplier) {
    this.authTokenSupplier = authTokenSupplier;
  }

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    requestContext
        .getHeaders()
        .add("Authorization", String.format("Token %s", authTokenSupplier.apply(requestContext)));
  }
}
