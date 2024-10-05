package pl.allegro.tech.hermes.test.helper.client;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
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
