package pl.allegro.tech.hermes.common.schema;

import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class SchemaRegistryClientFactory implements Factory<Client> {

    private final ConfigFactory configFactory;

    @Inject
    public SchemaRegistryClientFactory(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    @Override
    public Client provide() {
        int httpReadTimeoutMs = configFactory.getIntProperty(Configs.SCHEMA_REPOSITORY_HTTP_READ_TIMEOUT_MS);
        int httpConnectTimeoutMs = configFactory.getIntProperty(Configs.SCHEMA_REPOSITORY_HTTP_CONNECT_TIMEOUT_MS);

        ClientConfig config = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, httpReadTimeoutMs)
                .property(ClientProperties.CONNECT_TIMEOUT, httpConnectTimeoutMs);

        return ClientBuilder.newClient(config);
    }

    @Override
    public void dispose(Client instance) {
    }
}
