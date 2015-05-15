package pl.allegro.tech.hermes.client.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.HermesSender;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class MetricsHermesSender implements HermesSender {
    private final HermesSender sender;
    private final MetricRegistry metrics;

    public MetricsHermesSender(HermesSender sender, MetricRegistry metrics) {
        this.sender = sender;
        this.metrics = metrics;
    }

    @Override
    public CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
        String prefix = "hermes-client." + message.getSanitizedTopic();
        Timer.Context ctx = metrics.timer(prefix + ".latency").time();
        return sender.send(uri, message).whenComplete((resp, cause) -> {
            ctx.close();
            if (resp != null) {
                metrics.counter(prefix + ".status." + resp.getHttpStatus()).inc();
            }
        });
    }
}
