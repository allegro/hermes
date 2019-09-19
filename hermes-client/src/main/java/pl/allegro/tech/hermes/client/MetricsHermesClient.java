package pl.allegro.tech.hermes.client;

import net.jodah.failsafe.event.ExecutionAttemptedEvent;
import net.jodah.failsafe.event.ExecutionCompletedEvent;
import pl.allegro.tech.hermes.client.metrics.MetricsProvider;
import pl.allegro.tech.hermes.client.metrics.MetricsUtils;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

public class MetricsHermesClient extends HermesClient {

    private final MetricsProvider metrics;

    MetricsHermesClient(HermesSender sender,
                               URI uri,
                               Map<String, String> defaultHeaders,
                               int retries,
                               Predicate<HermesResponse> retryCondition,
                               long retrySleepInMillis,
                               long maxRetrySleepInMillis,
                               ScheduledExecutorService scheduler,
                               MetricsProvider metrics) {
        super(sender, uri, defaultHeaders, retries, retryCondition, retrySleepInMillis, maxRetrySleepInMillis, scheduler);
        this.metrics = metrics;
    }

    @Override
    protected void onMaxRetriesExceeded(ExecutionCompletedEvent<HermesResponse> event) {
        HermesMessage message = event.getResult().getHermesMessage();
        String prefix = MetricsUtils.getMetricsPrefix(message.getTopic());
        metrics.counterIncrement(prefix + ".retries.exhausted");
    }

    @Override
    protected void onFailedAttempt(ExecutionAttemptedEvent<HermesResponse> event) {
        HermesMessage message = event.getLastResult().getHermesMessage();
        String prefix = MetricsUtils.getMetricsPrefix(message.getTopic());
        metrics.counterIncrement(prefix + ".retries.count");
    }

    @Override
    protected void onSuccessfulRetry(ExecutionCompletedEvent<HermesResponse> event) {
        HermesMessage message = event.getResult().getHermesMessage();
        String prefix = MetricsUtils.getMetricsPrefix(message.getTopic());
        metrics.counterIncrement(prefix + ".retries.success");
        metrics.histogramUpdate(prefix + ".retries.attempts", event.getAttemptCount() - 1);
    }
}
