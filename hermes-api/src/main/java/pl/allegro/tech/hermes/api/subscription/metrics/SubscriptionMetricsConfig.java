package pl.allegro.tech.hermes.api.subscription.metrics;

import jakarta.validation.Valid;

public record SubscriptionMetricsConfig(
    @Valid MessageProcessingDurationMetricOptions messageProcessing) {

  public static final SubscriptionMetricsConfig DISABLED =
      new SubscriptionMetricsConfig(MessageProcessingDurationMetricOptions.DISABLED);
}
