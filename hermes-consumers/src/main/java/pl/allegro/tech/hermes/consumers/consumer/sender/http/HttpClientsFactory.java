package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.HttpCookieStore;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;

import java.util.concurrent.ExecutorService;

public class HttpClientsFactory {

    private final HttpClientParameters httpClientParameters;
    private final Http2ClientParameters http2ClientParameters;
    private final InstrumentedExecutorServiceFactory executorFactory;
    private final SslContextFactoryProvider sslContextFactoryProvider;

    public HttpClientsFactory(HttpClientParameters httpClientParameters,
                              Http2ClientParameters http2ClientParameters,
                              InstrumentedExecutorServiceFactory executorFactory,
                              SslContextFactoryProvider sslContextFactoryProvider) {
        this.httpClientParameters = httpClientParameters;
        this.http2ClientParameters = http2ClientParameters;
        this.executorFactory = executorFactory;
        this.sslContextFactoryProvider = sslContextFactoryProvider;
    }

    public HttpClient createClientForHttp1(String name) {
        ExecutorService executor = executorFactory.getExecutorService(
                name,
                httpClientParameters.getThreadPoolSize(),
                httpClientParameters.isThreadPoolMonitoringEnabled());

        HttpClient client = sslContextFactoryProvider.provideSslContextFactory()
                .map(HttpClient::new)
                .orElseGet(HttpClient::new);
        client.setMaxConnectionsPerDestination(httpClientParameters.getMaxConnectionsPerDestination());
        client.setMaxRequestsQueuedPerDestination(httpClientParameters.getMaxRequestsQueuedPerDestination());
        client.setExecutor(executor);
        client.setCookieStore(new HttpCookieStore.Empty());
        client.setIdleTimeout(httpClientParameters.getIdleTimeout());
        client.setFollowRedirects(httpClientParameters.isFollowRedirectsEnabled());
        return client;
    }

    public HttpClient createClientForHttp2() {
        ExecutorService executor = executorFactory.getExecutorService(
                "jetty-http2-client",
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
        client.setIdleTimeout(http2ClientParameters.getIdleTimeout());
        client.setFollowRedirects(httpClientParameters.isFollowRedirectsEnabled());
        return client;
    }
}
