package pl.allegro.tech.hermes.client.metrics;

import jersey.repackaged.com.google.common.collect.ImmutableMap;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.HermesSender;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class MetricsHermesSender implements HermesSender {
    private final HermesSender sender;
    private final MetricsProvider metrics;

    public MetricsHermesSender(HermesSender sender, MetricsProvider metrics) {
        this.sender = sender;
        this.metrics = metrics;
    }

    @Override
    public CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
        String prefix = MetricsUtils.getMetricsPrefix(message.getTopic());
        long startTime = System.nanoTime();

        return sender.send(uri, message).whenComplete((resp, cause) -> {
            metrics.timerRecord(prefix + ".latency", System.nanoTime() - startTime, NANOSECONDS);
            if (resp != null) {
                metrics.counterIncrement(prefix, "status",
                        ImmutableMap.of("code", String.valueOf(resp.getHttpStatus())));
            }
            if (cause != null) {
                metrics.counterIncrement(prefix + ".failure");
            }
        });
    }
}
