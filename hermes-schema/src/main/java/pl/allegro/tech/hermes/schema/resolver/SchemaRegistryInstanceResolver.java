package pl.allegro.tech.hermes.schema.resolver;

import javax.ws.rs.client.WebTarget;

public interface SchemaRegistryInstanceResolver {

    WebTarget resolve(String subject);
}
