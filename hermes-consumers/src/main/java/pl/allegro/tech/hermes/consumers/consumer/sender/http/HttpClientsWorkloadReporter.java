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

public class HttpClientsWorkloadReporter {

  private final MetricsFacade metrics;
  private final HttpClient http1SerialClient;
  private final HttpClient http1BatchClient;
  private final Http2ClientHolder http2ClientHolder;
  private final boolean isRequestQueueMonitoringEnabled;
  private final boolean isConnectionPoolMonitoringEnabled;

  public HttpClientsWorkloadReporter(
      MetricsFacade metrics,
      HttpClient http1SerialClient,
      HttpClient http1BatchClient,
      Http2ClientHolder http2ClientHolder,
      boolean isRequestQueueMonitoringEnabled,
      boolean isConnectionPoolMonitoringEnabled) {
    this.metrics = metrics;
    this.http1SerialClient = http1SerialClient;
    this.http1BatchClient = http1BatchClient;
    this.http2ClientHolder = http2ClientHolder;
    this.isRequestQueueMonitoringEnabled = isRequestQueueMonitoringEnabled;
    this.isConnectionPoolMonitoringEnabled = isConnectionPoolMonitoringEnabled;
  }

  public void start() {
    if (isRequestQueueMonitoringEnabled) {
      registerRequestQueueSizeGauges();
    }
    if (isConnectionPoolMonitoringEnabled) {
      registerConnectionGauges();
    }
  }

  private void registerRequestQueueSizeGauges() {
    metrics
        .consumerSender()
        .registerRequestQueueSizeGauge(this, HttpClientsWorkloadReporter::getQueuesSize);
    metrics
        .consumerSender()
        .registerHttp1SerialClientRequestQueueSizeGauge(
            this, HttpClientsWorkloadReporter::getHttp1SerialQueueSize);
    metrics
        .consumerSender()
        .registerHttp1BatchClientRequestQueueSizeGauge(
            this, HttpClientsWorkloadReporter::getHttp1BatchQueueSize);
    metrics
        .consumerSender()
        .registerHttp2RequestQueueSizeGauge(
            this, HttpClientsWorkloadReporter::getHttp2SerialQueueSize);
  }

  private void registerConnectionGauges() {
    metrics
        .consumerSender()
        .registerHttp1SerialClientActiveConnectionsGauge(
            this, HttpClientsWorkloadReporter::getHttp1SerialActiveConnections);
    metrics
        .consumerSender()
        .registerHttp1SerialClientIdleConnectionsGauge(
            this, HttpClientsWorkloadReporter::getHttp1SerialIdleConnections);
    metrics
        .consumerSender()
        .registerHttp1BatchClientActiveConnectionsGauge(
            this, HttpClientsWorkloadReporter::getHttp1BatchActiveConnections);
    metrics
        .consumerSender()
        .registerHttp1BatchClientIdleConnectionsGauge(
            this, HttpClientsWorkloadReporter::getHttp1BatchIdleConnections);
    metrics
        .consumerSender()
        .registerHttp2SerialClientConnectionsGauge(
            this, HttpClientsWorkloadReporter::getHttp2SerialConnections);
    metrics
        .consumerSender()
        .registerHttp2SerialClientPendingConnectionsGauge(
            this, HttpClientsWorkloadReporter::getHttp2SerialPendingConnections);
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
}
