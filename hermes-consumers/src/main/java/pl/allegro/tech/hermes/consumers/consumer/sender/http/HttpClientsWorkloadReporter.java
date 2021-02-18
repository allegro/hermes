package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.ConnectionPool;
import org.eclipse.jetty.client.DuplexConnectionPool;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.MultiplexConnectionPool;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.Gauges;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Stream;

public class HttpClientsWorkloadReporter {

    private final HermesMetrics metrics;
    private final HttpClient httpClient;
    private final Http2ClientHolder http2ClientHolder;
    private final boolean isConnectionPoolMonitoringEnabled;
    private final boolean isRequestQueueMonitoringEnabled;

    @Inject
    public HttpClientsWorkloadReporter(
            HermesMetrics metrics,
            @Named("http-1-client") HttpClient httpClient,
            Http2ClientHolder http2ClientHolder,
            ConfigFactory configFactory
    ) {
        this.metrics = metrics;
        this.httpClient = httpClient;
        this.http2ClientHolder = http2ClientHolder;
        this.isRequestQueueMonitoringEnabled = configFactory.getBooleanProperty(Configs.CONSUMER_HTTP_CLIENT_REQUEST_QUEUE_MONITORING_ENABLED);
        this.isConnectionPoolMonitoringEnabled = configFactory.getBooleanProperty(Configs.CONSUMER_HTTP_CLIENT_CONNECTION_POOL_MONITORING_ENABLED);
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
        metrics.registerConsumerSenderHttp1RequestQueueSize(this::getHttp1QueueSize);
        metrics.registerConsumerSenderHttp2RequestQueueSize(this::getHttp2QueueSize);
    }

    private void registerConnectionGauges() {
        metrics.registerGauge(Gauges.CONSUMER_SENDER_HTTP_1_ACTIVE_CONNECTIONS, () ->
                getHttp1ActiveConnectionsCount.apply(httpClient));
        metrics.registerGauge(Gauges.CONSUMER_SENDER_HTTP_1_IDLE_CONNECTIONS, () ->
                getHttp1IdleConnectionsCount.apply(httpClient));
        metrics.registerGauge(Gauges.CONSUMER_SENDER_HTTP_2_CONNECTIONS, () ->
                http2ClientHolder.getHttp2Client()
                        .map(getHttp2ConnectionsCount)
                        .orElse(0));
        metrics.registerGauge(Gauges.CONSUMER_SENDER_HTTP_2_PENDING_CONNECTIONS, () ->
                http2ClientHolder.getHttp2Client()
                        .map(getHttp2PendingConnectionsCount)
                        .orElse(0));
    }

    int getQueuesSize() {
        return getHttp1QueueSize() + getHttp2QueueSize();
    }

    int getHttp1QueueSize() {
        return getQueueSize.apply(httpClient);
    }

    int getHttp2QueueSize() {
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
