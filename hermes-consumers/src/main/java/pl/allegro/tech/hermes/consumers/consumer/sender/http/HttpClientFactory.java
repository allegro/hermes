package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.HttpCookieStore;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorService;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static pl.allegro.tech.hermes.common.config.Configs.*;

public class HttpClientFactory implements Factory<HttpClient> {

    private final ConfigFactory configFactory;
    private final HermesMetrics hermesMetrics;


    @Inject
    public HttpClientFactory(ConfigFactory configFactory, HermesMetrics hermesMetrics) {
        this.configFactory = configFactory;
        this.hermesMetrics = hermesMetrics;
    }

    @Override
    public HttpClient provide() {
        HttpClient client = new HttpClient();
        client.setMaxConnectionsPerDestination(configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_MAX_CONNECTIONS_PER_DESTINATION));
        client.setMaxRequestsQueuedPerDestination(configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_CONNECTIONS_QUEUE));
        client.setExecutor(getExecutor());
        client.setCookieStore(new HttpCookieStore.Empty());
        return client;
    }

    private ExecutorService getExecutor() {
        ExecutorService wrapped = Executors.newFixedThreadPool(configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_THREAD_POOL_SIZE));
        return new InstrumentedExecutorService(wrapped, hermesMetrics, "jetty-http-client");
    }


    @Override
    public void dispose(HttpClient instance) {
    }
}
