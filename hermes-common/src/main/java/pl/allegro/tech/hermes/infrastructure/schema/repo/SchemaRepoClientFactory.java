package pl.allegro.tech.hermes.infrastructure.schema.repo;

import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
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
        ClientConfig config = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, configFactory.getIntProperty(Configs.SCHEMA_REPOSITORY_HTTP_READ_TIMEOUT_MS))
                .property(ClientProperties.CONNECT_TIMEOUT, configFactory.getIntProperty(Configs.SCHEMA_REPOSITORY_HTTP_CONNECT_TIMEOUT_MS));

        return new JerseySchemaRepoClient(ClientBuilder.newClient(config), URI.create(configFactory.getStringProperty(Configs.SCHEMA_REPOSITORY_SERVER_URL)));
    }

    @Override
    public void dispose(SchemaRepoClient instance) {

    }
}
