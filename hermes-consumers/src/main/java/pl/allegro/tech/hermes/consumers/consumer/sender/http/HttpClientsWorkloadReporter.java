package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpDestination;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Stream;

public class HttpClientsWorkloadReporter {

    private final HermesMetrics metrics;
    private final List<HttpClient> clients = new ArrayList<>();

    @Inject
    public HttpClientsWorkloadReporter(
            HermesMetrics metrics,
            @Named("http-1-client") HttpClient httpClient,
            Http2ClientHolder http2ClientHolder
    ) {
        this.metrics = metrics;
        clients.add(httpClient);
        http2ClientHolder.getHttp2Client().ifPresent(clients::add);
    }

    public void registerMetrics() {
        metrics.registerConsumerSenderRequestQueueSize(this::getQueuesSize);
    }

    int getQueuesSize() {
        return clients.stream()
                .flatMap(getQueueSize)
                .reduce(0, (a, b) -> a + b);
    }

    private final Function<HttpClient, Stream<Integer>> getQueueSize = httpClient ->
            httpClient.getDestinations().stream()
                    .map(HttpDestination.class::cast)
                    .map(HttpDestination::getHttpExchanges)
                    .map(Queue::size);
}
