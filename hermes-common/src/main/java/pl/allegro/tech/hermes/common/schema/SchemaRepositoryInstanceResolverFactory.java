package pl.allegro.tech.hermes.common.schema;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.schema.resolver.DefaultSchemaRepositoryInstanceResolver;
import pl.allegro.tech.hermes.schema.resolver.SchemaRepositoryInstanceResolver;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import java.net.URI;

public class SchemaRepositoryInstanceResolverFactory implements Factory<SchemaRepositoryInstanceResolver> {

    private final ConfigFactory configFactory;
    private final Client client;

    @Inject
    public SchemaRepositoryInstanceResolverFactory(ConfigFactory configFactory, Client client) {
        this.configFactory = configFactory;
        this.client = client;
    }

    @Override
    public SchemaRepositoryInstanceResolver provide() {
        URI schemaRepositoryServerUri = URI.create(configFactory.getStringProperty(Configs.SCHEMA_REPOSITORY_SERVER_URL));
        return new DefaultSchemaRepositoryInstanceResolver(client, schemaRepositoryServerUri);
    }

    @Override
    public void dispose(SchemaRepositoryInstanceResolver instance) {
    }
}
