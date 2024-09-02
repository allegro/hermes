package pl.allegro.tech.hermes.client.metrics;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.HashMap;
import java.util.Map;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.MessageDeliveryListener;

public class MetricsMessageDeliveryListener implements MessageDeliveryListener {
  private final MetricsProvider metrics;

  public MetricsMessageDeliveryListener(MetricsProvider metrics) {
    this.metrics = metrics;
  }

  @Override
  public void onSend(HermesResponse response, long latency) {
    HermesMessage message = response.getHermesMessage();
    String topic = MetricsUtils.sanitizeTopic(message.getTopic());

    metrics.timerRecord(topic, "latency", latency, NANOSECONDS);
    Map<String, String> tags = new HashMap<>();
    tags.put("code", String.valueOf(response.getHttpStatus()));
    metrics.counterIncrement(topic, "status", tags);

    counterIncrementIf(topic, "publish.failure", response.isFailure());
  }

  @Override
  public void onFailure(HermesResponse response, int attemptCount) {
    String topic = MetricsUtils.sanitizeTopic(response.getHermesMessage().getTopic());
    metrics.counterIncrement(topic, "failure");
  }

  @Override
  public void onFailedRetry(HermesResponse response, int attemptCount) {
    String topic = MetricsUtils.sanitizeTopic(response.getHermesMessage().getTopic());
    metrics.counterIncrement(topic, "retries.count");
    metrics.counterIncrement(topic, "failure");

    metrics.counterIncrement(topic, "publish.retry.failure");
  }

  @Override
  public void onSuccessfulRetry(HermesResponse response, int attemptCount) {
    String topic = MetricsUtils.sanitizeTopic(response.getHermesMessage().getTopic());
    metrics.counterIncrement(topic, "retries.success");
    metrics.histogramUpdate(topic, "retries.attempts", attemptCount - 1);

    boolean wasRetried = attemptCount > 1;

    metrics.counterIncrement(topic, "publish.attempt");
    counterIncrementIf(topic, "publish.retry.success", response.isSuccess() && wasRetried);
    counterIncrementIf(topic, "publish.finally.success", response.isSuccess());
    counterIncrementIf(topic, "publish.retry.failure", response.isFailure() && wasRetried);
    counterIncrementIf(topic, "publish.finally.failure", response.isFailure());
    counterIncrementIf(topic, "publish.retry.attempt", wasRetried);
  }

  @Override
  public void onMaxRetriesExceeded(HermesResponse response, int attemptCount) {
    String topic = MetricsUtils.sanitizeTopic(response.getHermesMessage().getTopic());
    metrics.counterIncrement(topic, "retries.exhausted");
    metrics.counterIncrement(topic, "publish.finally.failure");
    metrics.counterIncrement(topic, "publish.attempt");
    metrics.counterIncrement(topic, "publish.retry.attempt");
  }

  private void counterIncrementIf(String topic, String name, boolean condition) {
    if (condition) {
      metrics.counterIncrement(topic, name);
    }
  }
}
