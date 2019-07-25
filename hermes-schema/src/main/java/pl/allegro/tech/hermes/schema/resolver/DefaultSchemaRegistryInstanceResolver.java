package pl.allegro.tech.hermes.schema.resolver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import java.net.URI;

public class DefaultSchemaRegistryInstanceResolver implements SchemaRegistryInstanceResolver {

    private final WebTarget target;

    public DefaultSchemaRegistryInstanceResolver(Client client, URI schemaRegistryServerUri) {
        this.target = client.target(schemaRegistryServerUri);
    }

    @Override
    public WebTarget resolve(String subject) {
        return target;
    }
}
