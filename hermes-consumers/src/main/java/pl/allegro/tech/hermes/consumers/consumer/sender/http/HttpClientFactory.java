package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.HttpCookieStore;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

import static pl.allegro.tech.hermes.common.config.Configs.*;

public class HttpClientFactory implements Factory<HttpClient> {

    private final ConfigFactory configFactory;
    private final InstrumentedExecutorServiceFactory executorFactory;

    @Inject
    public HttpClientFactory(ConfigFactory configFactory, InstrumentedExecutorServiceFactory executorFactory) {
        this.configFactory = configFactory;
        this.executorFactory = executorFactory;
    }

    @Override
    public HttpClient provide() {
        HttpClient client = new HttpClient();
        client.setMaxConnectionsPerDestination(configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_MAX_CONNECTIONS_PER_DESTINATION));
        client.setMaxRequestsQueuedPerDestination(configFactory.getIntProperty(CONSUMER_INFLIGHT_SIZE));
        client.setExecutor(getExecutor());
        client.setCookieStore(new HttpCookieStore.Empty());
        return client;
    }

    private ExecutorService getExecutor() {
        return executorFactory.getExecutorService("jetty-http-client", configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_THREAD_POOL_SIZE),
                                                  configFactory.getBooleanProperty(CONSUMER_HTTP_CLIENT_THREAD_POOL_MONITORING)
        );
    }

    @Override
    public void dispose(HttpClient instance) {
    }
}
