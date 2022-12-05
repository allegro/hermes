package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.HttpCookieStore;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;

import java.util.concurrent.ExecutorService;

public class HttpClientsFactory {

    private final InstrumentedExecutorServiceFactory executorFactory;
    private final SslContextFactoryProvider sslContextFactoryProvider;

    public HttpClientsFactory(
            InstrumentedExecutorServiceFactory executorFactory,
            SslContextFactoryProvider sslContextFactoryProvider) {
        this.executorFactory = executorFactory;
        this.sslContextFactoryProvider = sslContextFactoryProvider;
    }

    public HttpClient createClientForHttp1(String name, Http1ClientParameters http1ClientParameters) {
        ExecutorService executor = executorFactory.getExecutorService(
                name,
                http1ClientParameters.getThreadPoolSize(),
                http1ClientParameters.isThreadPoolMonitoringEnabled());

        HttpClient client = sslContextFactoryProvider.provideSslContextFactory()
                .map(HttpClient::new)
                .orElseGet(HttpClient::new);
        client.setMaxConnectionsPerDestination(http1ClientParameters.getMaxConnectionsPerDestination());
        client.setMaxRequestsQueuedPerDestination(http1ClientParameters.getMaxRequestsQueuedPerDestination());
        client.setExecutor(executor);
        client.setCookieStore(new HttpCookieStore.Empty());
        client.setIdleTimeout(http1ClientParameters.getIdleTimeout().toMillis());
        client.setFollowRedirects(http1ClientParameters.isFollowRedirectsEnabled());
        client.setConnectTimeout(http1ClientParameters.getConnectionTimeout().toMillis());
        return client;
    }

    public HttpClient createClientForHttp2(String name, Http2ClientParameters http2ClientParameters) {
        ExecutorService executor = executorFactory.getExecutorService(
                name,
                http2ClientParameters.getThreadPoolSize(),
                http2ClientParameters.isThreadPoolMonitoringEnabled());

        HTTP2Client http2Client = new HTTP2Client();
        http2Client.setExecutor(executor);
        HttpClientTransportOverHTTP2 transport = new HttpClientTransportOverHTTP2(http2Client);

        HttpClient client = sslContextFactoryProvider.provideSslContextFactory()
                .map(sslContextFactory -> new HttpClient(transport, sslContextFactory))
                .orElseThrow(() -> new IllegalStateException("Cannot create http/2 client due to lack of ssl context factory"));
        client.setMaxRequestsQueuedPerDestination(http2ClientParameters.getMaxRequestsQueuedPerDestination());
        client.setCookieStore(new HttpCookieStore.Empty());
        client.setIdleTimeout(http2ClientParameters.getIdleTimeout().toMillis());
        client.setFollowRedirects(http2ClientParameters.isFollowRedirectsEnabled());
        client.setConnectTimeout(http2ClientParameters.getConnectionTimeout().toMillis());
        return client;
    }
}
