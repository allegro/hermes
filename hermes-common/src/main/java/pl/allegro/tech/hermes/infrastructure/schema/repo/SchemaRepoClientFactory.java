package pl.allegro.tech.hermes.infrastructure.schema.repo;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import java.net.URI;

public class SchemaRepoClientFactory implements Factory<SchemaRepoClient> {

    private final ConfigFactory configFactory;

    @Inject
    public SchemaRepoClientFactory(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    @Override
    public SchemaRepoClient provide() {
        return new JerseySchemaRepoClient(ClientBuilder.newClient(), URI.create(configFactory.getStringProperty(Configs.SCHEMA_REPO_SERVER_URL)));
    }

    @Override
    public void dispose(SchemaRepoClient instance) {

    }
}
