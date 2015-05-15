package pl.allegro.tech.hermes.integration.env;

import java.util.concurrent.Executors;
import org.eclipse.jetty.client.HttpClient;

final class HttpClientFactory {

    private static final int MAX_CLIENT_CONNECTIONS_PER_DESTINATION = 10;

    private static final int CLIENT_THREAD_POOL_SIZE = 5;

    private HttpClientFactory() {
    }

    static HttpClient create() throws Exception {
        HttpClient client = new HttpClient();

        client.setMaxConnectionsPerDestination(MAX_CLIENT_CONNECTIONS_PER_DESTINATION);
        client.setExecutor(Executors.newFixedThreadPool(CLIENT_THREAD_POOL_SIZE));

        client.start();

        return client;
    }

}
