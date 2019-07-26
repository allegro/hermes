package pl.allegro.tech.hermes.common.schema;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.schema.resolver.DefaultSchemaRegistryInstanceResolver;
import pl.allegro.tech.hermes.schema.resolver.SchemaRegistryInstanceResolver;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import java.net.URI;

public class SchemaRegistryInstanceResolverFactory implements Factory<SchemaRegistryInstanceResolver> {

    private final ConfigFactory configFactory;
    private final Client client;

    @Inject
    public SchemaRegistryInstanceResolverFactory(ConfigFactory configFactory, Client client) {
        this.configFactory = configFactory;
        this.client = client;
    }

    @Override
    public SchemaRegistryInstanceResolver provide() {
        URI schemaRepositoryServerUri = URI.create(configFactory.getStringProperty(Configs.SCHEMA_REPOSITORY_SERVER_URL));
        return new DefaultSchemaRegistryInstanceResolver(client, schemaRepositoryServerUri);
    }

    @Override
    public void dispose(SchemaRegistryInstanceResolver instance) {
    }
}
