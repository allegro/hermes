package pl.allegro.tech.hermes.schema.resolver;

import jakarta.ws.rs.client.WebTarget;

public interface SchemaRepositoryInstanceResolver {

  WebTarget resolve(String subject);
}
