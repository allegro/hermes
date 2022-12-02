package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.ConnectionPool;
import org.eclipse.jetty.client.DuplexConnectionPool;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.MultiplexConnectionPool;
import pl.allegro.tech.hermes.common.metric.Gauges;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Stream;

public class HttpClientsWorkloadReporter {

    private final HermesMetrics metrics;
    private final HttpClient http1SerialClient;
    private final HttpClient http1BatchClient;
    private final Http2ClientHolder http2ClientHolder;
    private final boolean isRequestQueueMonitoringEnabled;
    private final boolean isConnectionPoolMonitoringEnabled;

    public HttpClientsWorkloadReporter(
            HermesMetrics metrics,
            HttpClient http1SerialClient,
            HttpClient http1BatchClient,
            Http2ClientHolder http2ClientHolder,
            boolean isRequestQueueMonitoringEnabled,
            boolean isConnectionPoolMonitoringEnabled
    ) {
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
        metrics.registerConsumerSenderRequestQueueSize(this::getQueuesSize);
        metrics.registerConsumerSenderHttp1SerialClientRequestQueueSize(this::getHttp1SerialClientQueueSize);
        metrics.registerConsumerSenderHttp1BatchClientRequestQueueSize(this::getHttp1BatchClientQueueSize);
        metrics.registerConsumerSenderHttp2RequestQueueSize(this::getHttp2SerialClientQueueSize);
    }

    private void registerConnectionGauges() {
        metrics.registerGauge(Gauges.CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_ACTIVE_CONNECTIONS, () ->
                getHttp1ActiveConnectionsCount.apply(http1SerialClient));
        metrics.registerGauge(Gauges.CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_IDLE_CONNECTIONS, () ->
                getHttp1IdleConnectionsCount.apply(http1SerialClient));

        metrics.registerGauge(Gauges.CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_ACTIVE_CONNECTIONS, () ->
                getHttp1ActiveConnectionsCount.apply(http1BatchClient));
        metrics.registerGauge(Gauges.CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_IDLE_CONNECTIONS, () ->
                getHttp1IdleConnectionsCount.apply(http1BatchClient));


        metrics.registerGauge(Gauges.CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_CONNECTIONS, () ->
                http2ClientHolder.getHttp2Client()
                        .map(getHttp2ConnectionsCount)
                        .orElse(0));
        metrics.registerGauge(Gauges.CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_PENDING_CONNECTIONS, () ->
                http2ClientHolder.getHttp2Client()
                        .map(getHttp2PendingConnectionsCount)
                        .orElse(0));
    }

    int getQueuesSize() {
        return getHttp1SerialClientQueueSize() + getHttp1BatchClientQueueSize() + getHttp2SerialClientQueueSize();
    }

    int getHttp1SerialClientQueueSize() {
        return getQueueSize.apply(http1SerialClient);
    }

    int getHttp1BatchClientQueueSize() {
        return getQueueSize.apply(http1BatchClient);
    }


    int getHttp2SerialClientQueueSize() {
        return http2ClientHolder.getHttp2Client()
                .map(getQueueSize)
                .orElse(0);
    }

    private final Function<HttpClient, Integer> getQueueSize = httpClient ->
            httpClient.getDestinations().stream()
                    .map(HttpDestination.class::cast)
                    .map(HttpDestination::getHttpExchanges)
                    .map(Queue::size)
                    .mapToInt(i -> i)
                    .sum();

    private final Function<HttpClient, Integer> getHttp1ActiveConnectionsCount = httpClient ->
            getHttp1ConnectionPool(httpClient)
                    .map(DuplexConnectionPool::getActiveConnectionCount)
                    .mapToInt(i -> i)
                    .sum();

    private final Function<HttpClient, Integer> getHttp1IdleConnectionsCount = httpClient ->
            getHttp1ConnectionPool(httpClient)
                    .map(DuplexConnectionPool::getIdleConnectionCount)
                    .mapToInt(i -> i)
                    .sum();

    private final Function<HttpClient, Integer> getHttp2ConnectionsCount = http2Client ->
            getHttp2ConnectionPool(http2Client)
                    .map(MultiplexConnectionPool::getConnectionCount)
                    .mapToInt(i -> i)
                    .sum();

    private final Function<HttpClient, Integer> getHttp2PendingConnectionsCount = http2Client ->
            getHttp2ConnectionPool(http2Client)
                    .map(MultiplexConnectionPool::getPendingCount)
                    .mapToInt(i -> i)
                    .sum();

    private Stream<ConnectionPool> getConnectionPool(HttpClient httpClient) {
        return httpClient.getDestinations().stream()
                .map(HttpDestination.class::cast)
                .map(HttpDestination::getConnectionPool);
    }

    private Stream<DuplexConnectionPool> getHttp1ConnectionPool(HttpClient httpClient) {
        return getConnectionPool(httpClient)
                .map(DuplexConnectionPool.class::cast);
    }

    private Stream<MultiplexConnectionPool> getHttp2ConnectionPool(HttpClient http2Client) {
        return getConnectionPool(http2Client)
                .map(MultiplexConnectionPool.class::cast);
    }

}
