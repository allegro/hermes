package pl.allegro.tech.hermes.common.schema;

import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.schema.confluent.SchemaRegistrySchemaSourceClient;
import pl.allegro.tech.hermes.schema.schemarepo.SchemaRepoSchemaSourceClient;
import pl.allegro.tech.hermes.schema.SchemaSourceClient;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URI;

public class SchemaSourceClientFactory implements Factory<SchemaSourceClient> {

    private final ConfigFactory configFactory;

    @Inject
    public SchemaSourceClientFactory(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    @Override
    public SchemaSourceClient provide() {

        int httpReadTimeoutMs = configFactory.getIntProperty(Configs.SCHEMA_REPOSITORY_HTTP_READ_TIMEOUT_MS);
        int httpConnectTimeoutMs = configFactory.getIntProperty(Configs.SCHEMA_REPOSITORY_HTTP_CONNECT_TIMEOUT_MS);

        ClientConfig config = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, httpReadTimeoutMs)
                .property(ClientProperties.CONNECT_TIMEOUT, httpConnectTimeoutMs);

        String schemaRepositoryType = configFactory.getStringProperty(Configs.SCHEMA_REPOSITORY_TYPE).toUpperCase();
        Client client = ClientBuilder.newClient(config);
        URI schemaRepositoryServerUri = URI.create(configFactory.getStringProperty(Configs.SCHEMA_REPOSITORY_SERVER_URL));
        switch (SchemaRepositoryType.valueOf(schemaRepositoryType)) {
            case SCHEMA_REPO:
                return new SchemaRepoSchemaSourceClient(client, schemaRepositoryServerUri);
            case SCHEMA_REGISTRY:
                return new SchemaRegistrySchemaSourceClient(client, schemaRepositoryServerUri);
            default:
                throw new IllegalStateException("Unknown schema repository type " + schemaRepositoryType);
        }
    }

    @Override
    public void dispose(SchemaSourceClient instance) {

    }
}
