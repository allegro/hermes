package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Stream;
import org.eclipse.jetty.client.ConnectionPool;
import org.eclipse.jetty.client.DuplexConnectionPool;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.MultiplexConnectionPool;
import org.eclipse.jetty.client.transport.HttpDestination;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.metrics.HermesTimer;

public class HttpClientsMetricsReporter {

  private final MetricsFacade metrics;
  private final HttpClient http1SerialClient;
  private final HttpClient http1BatchClient;
  private final Http2ClientHolder http2ClientHolder;
  private final boolean isRequestQueueMonitoringEnabled;
  private final boolean isConnectionPoolMonitoringEnabled;
  private final boolean isRequestProcessingMonitoringEnabled;

  public HttpClientsMetricsReporter(
      MetricsFacade metrics,
      HttpClient http1SerialClient,
      HttpClient http1BatchClient,
      Http2ClientHolder http2ClientHolder,
      boolean isRequestQueueMonitoringEnabled,
      boolean isConnectionPoolMonitoringEnabled,
      boolean isRequestProcessingMonitoringEnabled) {
    this.metrics = metrics;
    this.http1SerialClient = http1SerialClient;
    this.http1BatchClient = http1BatchClient;
    this.http2ClientHolder = http2ClientHolder;
    this.isRequestQueueMonitoringEnabled = isRequestQueueMonitoringEnabled;
    this.isConnectionPoolMonitoringEnabled = isConnectionPoolMonitoringEnabled;
    this.isRequestProcessingMonitoringEnabled = isRequestProcessingMonitoringEnabled;
  }

  public void start() {
    if (isRequestQueueMonitoringEnabled) {
      registerRequestQueueSizeGauges();
    }
    if (isConnectionPoolMonitoringEnabled) {
      registerConnectionGauges();
    }
    if (isRequestProcessingMonitoringEnabled) {
      registerRequestProcessingListeners();
    }
  }

  private void registerRequestQueueSizeGauges() {
    metrics
        .consumerSender()
        .registerRequestQueueSizeGauge(this, HttpClientsMetricsReporter::getQueuesSize);
    metrics
        .consumerSender()
        .registerHttp1SerialClientRequestQueueSizeGauge(
            this, HttpClientsMetricsReporter::getHttp1SerialQueueSize);
    metrics
        .consumerSender()
        .registerHttp1BatchClientRequestQueueSizeGauge(
            this, HttpClientsMetricsReporter::getHttp1BatchQueueSize);
    metrics
        .consumerSender()
        .registerHttp2RequestQueueSizeGauge(
            this, HttpClientsMetricsReporter::getHttp2SerialQueueSize);
  }

  private void registerConnectionGauges() {
    metrics
        .consumerSender()
        .registerHttp1SerialClientActiveConnectionsGauge(
            this, HttpClientsMetricsReporter::getHttp1SerialActiveConnections);
    metrics
        .consumerSender()
        .registerHttp1SerialClientIdleConnectionsGauge(
            this, HttpClientsMetricsReporter::getHttp1SerialIdleConnections);
    metrics
        .consumerSender()
        .registerHttp1BatchClientActiveConnectionsGauge(
            this, HttpClientsMetricsReporter::getHttp1BatchActiveConnections);
    metrics
        .consumerSender()
        .registerHttp1BatchClientIdleConnectionsGauge(
            this, HttpClientsMetricsReporter::getHttp1BatchIdleConnections);
    metrics
        .consumerSender()
        .registerHttp2SerialClientConnectionsGauge(
            this, HttpClientsMetricsReporter::getHttp2SerialConnections);
    metrics
        .consumerSender()
        .registerHttp2SerialClientPendingConnectionsGauge(
            this, HttpClientsMetricsReporter::getHttp2SerialPendingConnections);
  }

  int getQueuesSize() {
    return getHttp1SerialQueueSize() + getHttp1BatchQueueSize() + getHttp2SerialQueueSize();
  }

  int getHttp1SerialQueueSize() {
    return getQueueSize.apply(http1SerialClient);
  }

  int getHttp1BatchQueueSize() {
    return getQueueSize.apply(http1BatchClient);
  }

  int getHttp2SerialQueueSize() {
    return http2ClientHolder.getHttp2Client().map(getQueueSize).orElse(0);
  }

  private int getHttp1SerialActiveConnections() {
    return getHttp1ActiveConnectionsCount.apply(http1SerialClient);
  }

  private int getHttp1SerialIdleConnections() {
    return getHttp1IdleConnectionsCount.apply(http1SerialClient);
  }

  private int getHttp1BatchActiveConnections() {
    return getHttp1ActiveConnectionsCount.apply(http1BatchClient);
  }

  private int getHttp1BatchIdleConnections() {
    return getHttp1IdleConnectionsCount.apply(http1BatchClient);
  }

  private int getHttp2SerialConnections() {
    return http2ClientHolder.getHttp2Client().map(getHttp2ConnectionsCount).orElse(0);
  }

  private int getHttp2SerialPendingConnections() {
    return http2ClientHolder.getHttp2Client().map(getHttp2PendingConnectionsCount).orElse(0);
  }

  private final Function<HttpClient, Integer> getQueueSize =
      httpClient ->
          httpClient.getDestinations().stream()
              .map(HttpDestination.class::cast)
              .map(HttpDestination::getHttpExchanges)
              .map(Queue::size)
              .mapToInt(i -> i)
              .sum();

  private final Function<HttpClient, Integer> getHttp1ActiveConnectionsCount =
      httpClient ->
          getHttp1ConnectionPool(httpClient)
              .map(DuplexConnectionPool::getActiveConnectionCount)
              .mapToInt(i -> i)
              .sum();

  private final Function<HttpClient, Integer> getHttp1IdleConnectionsCount =
      httpClient ->
          getHttp1ConnectionPool(httpClient)
              .map(DuplexConnectionPool::getIdleConnectionCount)
              .mapToInt(i -> i)
              .sum();

  private final Function<HttpClient, Integer> getHttp2ConnectionsCount =
      http2Client ->
          getHttp2ConnectionPool(http2Client)
              .map(MultiplexConnectionPool::getConnectionCount)
              .mapToInt(i -> i)
              .sum();

  private final Function<HttpClient, Integer> getHttp2PendingConnectionsCount =
      http2Client ->
          getHttp2ConnectionPool(http2Client)
              .map(MultiplexConnectionPool::getPendingConnectionCount)
              .mapToInt(i -> i)
              .sum();

  private Stream<ConnectionPool> getConnectionPool(HttpClient httpClient) {
    return httpClient.getDestinations().stream()
        .map(HttpDestination.class::cast)
        .map(HttpDestination::getConnectionPool);
  }

  private Stream<DuplexConnectionPool> getHttp1ConnectionPool(HttpClient httpClient) {
    return getConnectionPool(httpClient).map(DuplexConnectionPool.class::cast);
  }

  private Stream<MultiplexConnectionPool> getHttp2ConnectionPool(HttpClient http2Client) {
    return getConnectionPool(http2Client).map(MultiplexConnectionPool.class::cast);
  }

  private void registerRequestProcessingListeners() {
    enrichWithRequestProcessingMetrics(
        http1SerialClient,
        metrics.consumerSender().http1SerialClientRequestQueueWaitingTimer(),
        metrics.consumerSender().http1SerialClientRequestProcessingTimer());
    enrichWithRequestProcessingMetrics(
        http1BatchClient,
        metrics.consumerSender().http1BatchClientRequestQueueWaitingTimer(),
        metrics.consumerSender().http1BatchClientRequestProcessingTimer());
    http2ClientHolder
        .getHttp2Client()
        .ifPresent(
            http2Client ->
                enrichWithRequestProcessingMetrics(
                    http2Client,
                    metrics.consumerSender().http2SerialClientRequestQueueWaitingTimer(),
                    metrics.consumerSender().http2SerialClientRequestProcessingTimer()));
  }

  private static void enrichWithRequestProcessingMetrics(
      HttpClient client, HermesTimer requestQueueWaitingTimer, HermesTimer requestProcessingTimer) {
    client
        .getRequestListeners()
        .addListener(new JettyHttpClientMetrics(requestQueueWaitingTimer, requestProcessingTimer));
  }
}
