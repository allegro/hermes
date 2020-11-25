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

        counterIncrementIf(prefix + ".publish.failure", response.isFailure());
    }

    @Override
    public void onFailure(HermesResponse response, int attemptCount) {
        String prefix = MetricsUtils.getMetricsPrefix(response.getHermesMessage().getTopic());
        metrics.counterIncrement(prefix + ".failure");
    }

    @Override
    public void onFailedRetry(HermesResponse response, int attemptCount) {
        String prefix = MetricsUtils.getMetricsPrefix(response.getHermesMessage().getTopic());
        metrics.counterIncrement(prefix + ".retries.count");
        metrics.counterIncrement(prefix + ".failure");

        metrics.counterIncrement(prefix + ".publish.retry.failure");
    }

    @Override
    public void onSuccessfulRetry(HermesResponse response, int attemptCount) {
        String prefix = MetricsUtils.getMetricsPrefix(response.getHermesMessage().getTopic());
        metrics.counterIncrement(prefix + ".retries.success");
        metrics.histogramUpdate(prefix + ".retries.attempts", attemptCount - 1);

        boolean wasRetried = attemptCount > 1;

        metrics.counterIncrement(prefix + ".publish.attempt");
        counterIncrementIf(prefix + ".publish.retry.success", response.isSuccess() && wasRetried);
        counterIncrementIf(prefix + ".publish.finally.success", response.isSuccess());
        counterIncrementIf(prefix + ".publish.retry.failure", response.isFailure() && wasRetried);
        counterIncrementIf(prefix + ".publish.finally.failure", response.isFailure());
        counterIncrementIf(prefix + ".publish.retry.attempt", wasRetried);
    }

    @Override
    public void onMaxRetriesExceeded(HermesResponse response, int attemptCount) {
        String prefix = MetricsUtils.getMetricsPrefix(response.getHermesMessage().getTopic());
        metrics.counterIncrement(prefix + ".retries.exhausted");
        metrics.counterIncrement(prefix + ".publish.finally.failure");
        metrics.counterIncrement(prefix + ".publish.attempt");
        metrics.counterIncrement(prefix + ".publish.retry.attempt");
    }

    private void counterIncrementIf(String name, boolean condition) {
        if (condition) {
            metrics.counterIncrement(name);
        }
    }
}
