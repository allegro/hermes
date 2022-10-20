package pl.allegro.tech.hermes.schema.resolver;

import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

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
