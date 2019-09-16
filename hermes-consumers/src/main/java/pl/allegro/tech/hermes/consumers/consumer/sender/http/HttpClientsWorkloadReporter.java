package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpDestination;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Queue;
import java.util.function.Function;

public class HttpClientsWorkloadReporter {

    private final HermesMetrics metrics;
    private final HttpClient httpClient;
    private final Http2ClientHolder http2ClientHolder;

    @Inject
    public HttpClientsWorkloadReporter(
            HermesMetrics metrics,
            @Named("http-1-client") HttpClient httpClient,
            Http2ClientHolder http2ClientHolder
    ) {
        this.metrics = metrics;
        this.httpClient = httpClient;
        this.http2ClientHolder = http2ClientHolder;
    }

    public void registerMetrics() {
        metrics.registerConsumerSenderRequestQueueSize(this::getQueuesSize);
        metrics.registerConsumerSenderHttp1RequestQueueSize(this::getHttp1QueueSize);
        metrics.registerConsumerSenderHttp2RequestQueueSize(this::getHttp2QueueSize);
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
}
