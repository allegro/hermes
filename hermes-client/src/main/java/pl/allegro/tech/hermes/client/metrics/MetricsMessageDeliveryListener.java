package pl.allegro.tech.hermes.client.metrics;

import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.MessageDeliveryListener;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class MetricsMessageDeliveryListener implements MessageDeliveryListener {
    private final MetricsProvider metrics;

    public MetricsMessageDeliveryListener(MetricsProvider metrics) {
        this.metrics = metrics;
    }

    @Override
    public void onSend(HermesResponse response, long latency) {
        HermesMessage message = response.getHermesMessage();
        String prefix = MetricsUtils.getMetricsPrefix(message.getTopic());

        metrics.timerRecord(prefix + ".latency", latency, NANOSECONDS);
        Map<String, String> tags = new HashMap<>();
        tags.put("code", String.valueOf(response.getHttpStatus()));
        metrics.counterIncrement(prefix, "status", tags);
    }

    @Override
    public void onFailure(HermesMessage message, int attemptCount) {
        String prefix = MetricsUtils.getMetricsPrefix(message.getTopic());
        metrics.counterIncrement(prefix + ".failure");
    }

    @Override
    public void onFailedRetry(HermesMessage message, int attemptCount) {
        String prefix = MetricsUtils.getMetricsPrefix(message.getTopic());
        metrics.counterIncrement(prefix + ".retries.count");
        metrics.counterIncrement(prefix + ".failure");
    }

    @Override
    public void onSuccessfulRetry(HermesMessage message, int attemptCount) {
        String prefix = MetricsUtils.getMetricsPrefix(message.getTopic());
        metrics.counterIncrement(prefix + ".retries.success");
        metrics.histogramUpdate(prefix + ".retries.attempts", attemptCount - 1);
    }

    @Override
    public void onMaxRetriesExceeded(HermesMessage message, int attemptCount) {
        String prefix = MetricsUtils.getMetricsPrefix(message.getTopic());
        metrics.counterIncrement(prefix + ".retries.exhausted");
    }
}
