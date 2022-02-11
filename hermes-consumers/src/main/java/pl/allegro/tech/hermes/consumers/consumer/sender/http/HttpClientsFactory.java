package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.HttpCookieStore;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;

import java.util.concurrent.ExecutorService;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP2_CLIENT_IDLE_TIMEOUT;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP2_CLIENT_MAX_REQUESTS_QUEUED_PER_DESTINATION;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP2_CLIENT_THREAD_POOL_MONITORING;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP2_CLIENT_THREAD_POOL_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_FOLLOW_REDIRECTS;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_IDLE_TIMEOUT;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_MAX_CONNECTIONS_PER_DESTINATION;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_MAX_REQUESTS_QUEUED_PER_DESTINATION;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_THREAD_POOL_MONITORING;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_THREAD_POOL_SIZE;

public class HttpClientsFactory {

    private final ConfigFactory configFactory;
    private final InstrumentedExecutorServiceFactory executorFactory;
    private final SslContextFactoryProvider sslContextFactoryProvider;

    public HttpClientsFactory(ConfigFactory configFactory,
                              InstrumentedExecutorServiceFactory executorFactory,
                              SslContextFactoryProvider sslContextFactoryProvider) {
        this.configFactory = configFactory;
        this.executorFactory = executorFactory;
        this.sslContextFactoryProvider = sslContextFactoryProvider;
    }

    public HttpClient createClientForHttp1(String name) {
        ExecutorService executor = executorFactory.getExecutorService(
                name,
                configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_THREAD_POOL_SIZE),
                configFactory.getBooleanProperty(CONSUMER_HTTP_CLIENT_THREAD_POOL_MONITORING));

        HttpClient client = sslContextFactoryProvider.provideSslContextFactory()
                .map(sslContextFactory -> new HttpClient(sslContextFactory))
                .orElseGet(() -> new HttpClient());
        client.setMaxConnectionsPerDestination(configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_MAX_CONNECTIONS_PER_DESTINATION));
        client.setMaxRequestsQueuedPerDestination(configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_MAX_REQUESTS_QUEUED_PER_DESTINATION));
        client.setExecutor(executor);
        client.setCookieStore(new HttpCookieStore.Empty());
        client.setIdleTimeout(configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_IDLE_TIMEOUT));
        client.setFollowRedirects(configFactory.getBooleanProperty(CONSUMER_HTTP_CLIENT_FOLLOW_REDIRECTS));
        return client;
    }

    public HttpClient createClientForHttp2() {
        ExecutorService executor = executorFactory.getExecutorService(
                "jetty-http2-client",
                configFactory.getIntProperty(CONSUMER_HTTP2_CLIENT_THREAD_POOL_SIZE),
                configFactory.getBooleanProperty(CONSUMER_HTTP2_CLIENT_THREAD_POOL_MONITORING));

        HTTP2Client http2Client = new HTTP2Client();
        http2Client.setExecutor(executor);
        HttpClientTransportOverHTTP2 transport = new HttpClientTransportOverHTTP2(http2Client);

        HttpClient client = sslContextFactoryProvider.provideSslContextFactory()
                .map(sslContextFactory -> new HttpClient(transport, sslContextFactory))
                .orElseThrow(() -> new IllegalStateException("Cannot create http/2 client due to lack of ssl context factory"));
        client.setMaxRequestsQueuedPerDestination(configFactory.getIntProperty(CONSUMER_HTTP2_CLIENT_MAX_REQUESTS_QUEUED_PER_DESTINATION));
        client.setCookieStore(new HttpCookieStore.Empty());
        client.setIdleTimeout(configFactory.getIntProperty(CONSUMER_HTTP2_CLIENT_IDLE_TIMEOUT));
        client.setFollowRedirects(configFactory.getBooleanProperty(CONSUMER_HTTP_CLIENT_FOLLOW_REDIRECTS));
        return client;
    }
}
