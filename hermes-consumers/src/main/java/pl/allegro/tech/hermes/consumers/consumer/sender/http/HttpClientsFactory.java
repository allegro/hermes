package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.util.concurrent.ExecutorService;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.transport.HttpClientTransportOverHTTP;
import org.eclipse.jetty.http.HttpCookieStore;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.transport.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.io.ClientConnector;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;

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
    ClientConnector clientConnector = new ClientConnector();
    sslContextFactoryProvider
        .provideSslContextFactory()
        .ifPresent(clientConnector::setSslContextFactory);
    HttpClientTransportOverHTTP transport = new HttpClientTransportOverHTTP(clientConnector);
    HttpClient client = new HttpClient(transport);

    ExecutorService executor =
        executorFactory.getExecutorService(
            name,
            http1ClientParameters.getThreadPoolSize(),
            http1ClientParameters.isThreadPoolMonitoringEnabled());
    client.setExecutor(executor);
    client.setMaxConnectionsPerDestination(http1ClientParameters.getMaxConnectionsPerDestination());
    client.setMaxRequestsQueuedPerDestination(
        http1ClientParameters.getMaxRequestsQueuedPerDestination());
    client.setHttpCookieStore(new HttpCookieStore.Empty());
    client.setIdleTimeout(http1ClientParameters.getIdleTimeout().toMillis());
    client.setFollowRedirects(http1ClientParameters.isFollowRedirectsEnabled());
    client.setConnectTimeout(http1ClientParameters.getConnectionTimeout().toMillis());
    return client;
  }

  public HttpClient createClientForHttp2(String name, Http2ClientParameters http2ClientParameters) {
    ClientConnector clientConnector = new ClientConnector();
    sslContextFactoryProvider
        .provideSslContextFactory()
        .ifPresentOrElse(
            clientConnector::setSslContextFactory,
            () -> {
              throw new IllegalStateException(
                  "Cannot create http/2 client due to lack of ssl context factory");
            });
    HTTP2Client http2Client = new HTTP2Client(clientConnector);

    ExecutorService executor =
        executorFactory.getExecutorService(
            name,
            http2ClientParameters.getThreadPoolSize(),
            http2ClientParameters.isThreadPoolMonitoringEnabled());
    http2Client.setExecutor(executor);

    HttpClientTransportOverHTTP2 transport = new HttpClientTransportOverHTTP2(http2Client);
    HttpClient client = new HttpClient(transport);

    client.setMaxRequestsQueuedPerDestination(
        http2ClientParameters.getMaxRequestsQueuedPerDestination());
    client.setHttpCookieStore(new HttpCookieStore.Empty());
    client.setIdleTimeout(http2ClientParameters.getIdleTimeout().toMillis());
    client.setFollowRedirects(http2ClientParameters.isFollowRedirectsEnabled());
    client.setConnectTimeout(http2ClientParameters.getConnectionTimeout().toMillis());
    return client;
  }
}
