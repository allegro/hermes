package pl.allegro.tech.hermes.test.helper.endpoint;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

public class JerseyClientFactory {

    public static Client create() {
        return ClientBuilder.newClient(createConfig());
    }

    static ClientConfig createConfig() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.ASYNC_THREADPOOL_SIZE, 10);
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, 5000);
        clientConfig.property(ClientProperties.READ_TIMEOUT, 80000);
        return clientConfig;
    }

}
