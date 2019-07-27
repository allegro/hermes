package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP2_CLIENT_IDLE_TIMEOUT;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP2_CLIENT_THREAD_POOL_MONITORING;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP2_CLIENT_THREAD_POOL_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_IDLE_TIMEOUT;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_MAX_CONNECTIONS_PER_DESTINATION;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_THREAD_POOL_MONITORING;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_THREAD_POOL_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_INFLIGHT_SIZE;

public class HttpClientFactory implements Factory<HttpClient> {

    private final ConfigFactory configFactory;
    private final InstrumentedExecutorServiceFactory executorFactory;
    private final pl.allegro.tech.hermes.common.ssl.SslContextFactory sslContextFactory;

    @Inject
    public HttpClientFactory(ConfigFactory configFactory,
                             InstrumentedExecutorServiceFactory executorFactory,
                             SslContextFactoryProvider sslContextFactoryProvider) {
        this.configFactory = configFactory;
        this.executorFactory = executorFactory;
        this.sslContextFactory = sslContextFactoryProvider.getSslContextFactory();
    }

    @Override
    public HttpClient provide() {
        return createClientForHttp1("jetty-http-client");
    }

    public HttpClient createClientForHttp1(String name) {
        ExecutorService executor = executorFactory.getExecutorService(name,
                configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_THREAD_POOL_SIZE),
                configFactory.getBooleanProperty(CONSUMER_HTTP_CLIENT_THREAD_POOL_MONITORING));

        HttpClient client = new HttpClient(createSslContextFactory());
        client.setMaxConnectionsPerDestination(configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_MAX_CONNECTIONS_PER_DESTINATION));
        client.setMaxRequestsQueuedPerDestination(configFactory.getIntProperty(CONSUMER_INFLIGHT_SIZE));
        client.setExecutor(executor);
        client.setCookieStore(new HttpCookieStore.Empty());
        client.setIdleTimeout(configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_IDLE_TIMEOUT));
        return client;
    }

    public HttpClient createClientForHttp2() {
        ExecutorService executor = executorFactory.getExecutorService("jetty-http2-client",
                configFactory.getIntProperty(CONSUMER_HTTP2_CLIENT_THREAD_POOL_SIZE),
                configFactory.getBooleanProperty(CONSUMER_HTTP2_CLIENT_THREAD_POOL_MONITORING));


        HTTP2Client h2Client = new HTTP2Client();
        h2Client.setExecutor(executor);
        HttpClientTransportOverHTTP2 transport = new HttpClientTransportOverHTTP2(h2Client);

        HttpClient client = new HttpClient(transport, createSslContextFactory());
        client.setMaxRequestsQueuedPerDestination(configFactory.getIntProperty(CONSUMER_INFLIGHT_SIZE));
        client.setCookieStore(new HttpCookieStore.Empty());
        client.setIdleTimeout(configFactory.getIntProperty(CONSUMER_HTTP2_CLIENT_IDLE_TIMEOUT));
        return client;
    }

    private SslContextFactory createSslContextFactory() {
        SslContextFactory sslCtx = new SslContextFactory();
        sslCtx.setEndpointIdentificationAlgorithm("HTTPS");
        sslCtx.setSslContext(sslContextFactory.create().getSslContext());
        return sslCtx;
    }

    @Override
    public void dispose(HttpClient instance) {
    }


}
