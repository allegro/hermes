package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.HttpCookieStore;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;

import javax.inject.Inject;
import java.util.concurrent.Executors;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_CONNECTIONS_QUEUE;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_MAX_CONNECTIONS_PER_DESTINATION;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_THREAD_POOL_SIZE;

public class HttpClientFactory implements Factory<HttpClient> {

    private final ConfigFactory configFactory;

    @Inject
    public HttpClientFactory(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    @Override
    public HttpClient provide() {
        HttpClient client = new HttpClient();
        client.setMaxConnectionsPerDestination(configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_MAX_CONNECTIONS_PER_DESTINATION));
        client.setMaxRequestsQueuedPerDestination(configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_CONNECTIONS_QUEUE));
        client.setExecutor(Executors.newFixedThreadPool(configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_THREAD_POOL_SIZE)));
        client.setCookieStore(new HttpCookieStore.Empty());
        return client;
    }

    @Override
    public void dispose(HttpClient instance) {
    }
}
