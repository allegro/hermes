package pl.allegro.tech.hermes.schema.resolver;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import java.net.URI;

public class DefaultSchemaRepositoryInstanceResolver implements SchemaRepositoryInstanceResolver {

  private final WebTarget target;

  public DefaultSchemaRepositoryInstanceResolver(Client client, URI schemaRegistryServerUri) {
    this.target = client.target(schemaRegistryServerUri);
  }

  @Override
  public WebTarget resolve(String subject) {
    return target;
  }
}
