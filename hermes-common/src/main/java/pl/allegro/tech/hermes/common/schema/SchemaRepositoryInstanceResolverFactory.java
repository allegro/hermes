package pl.allegro.tech.hermes.common.schema;

import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.schema.resolver.DefaultSchemaRepositoryInstanceResolver;
import pl.allegro.tech.hermes.schema.resolver.SchemaRepositoryInstanceResolver;

import javax.ws.rs.client.Client;
import java.net.URI;

public class SchemaRepositoryInstanceResolverFactory {

    private final ConfigFactory configFactory;
    private final Client client;

    public SchemaRepositoryInstanceResolverFactory(ConfigFactory configFactory, Client client) {
        this.configFactory = configFactory;
        this.client = client;
    }

    public SchemaRepositoryInstanceResolver provide() {
        URI schemaRepositoryServerUri = URI.create(configFactory.getStringProperty(Configs.SCHEMA_REPOSITORY_SERVER_URL));
        return new DefaultSchemaRepositoryInstanceResolver(client, schemaRepositoryServerUri);
    }
}
