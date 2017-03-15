package pl.allegro.tech.hermes.common.schema;

import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.confluent.SchemaRegistryRawSchemaClient;
import pl.allegro.tech.hermes.schema.schemarepo.SchemaRepoRawSchemaClient;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URI;

public class RawSchemaClientFactory implements Factory<RawSchemaClient> {

    private final ConfigFactory configFactory;
    private final HermesMetrics hermesMetrics;

    @Inject
    public RawSchemaClientFactory(ConfigFactory configFactory, HermesMetrics hermesMetrics) {
        this.configFactory = configFactory;
        this.hermesMetrics = hermesMetrics;
    }

    @Override
    public RawSchemaClient provide() {

        int httpReadTimeoutMs = configFactory.getIntProperty(Configs.SCHEMA_REPOSITORY_HTTP_READ_TIMEOUT_MS);
        int httpConnectTimeoutMs = configFactory.getIntProperty(Configs.SCHEMA_REPOSITORY_HTTP_CONNECT_TIMEOUT_MS);

        ClientConfig config = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, httpReadTimeoutMs)
                .property(ClientProperties.CONNECT_TIMEOUT, httpConnectTimeoutMs);

        String schemaRepositoryType = configFactory.getStringProperty(Configs.SCHEMA_REPOSITORY_TYPE).toUpperCase();
        Client client = ClientBuilder.newClient(config);
        URI schemaRepositoryServerUri = URI.create(configFactory.getStringProperty(Configs.SCHEMA_REPOSITORY_SERVER_URL));
        SchemaRepositoryType repoType = SchemaRepositoryType.valueOf(schemaRepositoryType);
        switch (repoType) {
            case SCHEMA_REPO:
                return createMetricsTrackingClient(new SchemaRepoRawSchemaClient(client, schemaRepositoryServerUri), repoType);
            case SCHEMA_REGISTRY:
                return createMetricsTrackingClient(new SchemaRegistryRawSchemaClient(client, schemaRepositoryServerUri), repoType);
            default:
                throw new IllegalStateException("Unknown schema repository type " + schemaRepositoryType);
        }
    }

    private RawSchemaClient createMetricsTrackingClient(RawSchemaClient rawSchemaClient, SchemaRepositoryType schemaRepositoryType) {
        return new ReadMetricsTrackingRawSchemaClient(rawSchemaClient, hermesMetrics, schemaRepositoryType);
    }

    @Override
    public void dispose(RawSchemaClient instance) {

    }
}
