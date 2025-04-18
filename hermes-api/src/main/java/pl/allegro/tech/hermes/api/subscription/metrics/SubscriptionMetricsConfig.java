package pl.allegro.tech.hermes.api.subscription.metrics;

import jakarta.validation.Valid;

public record SubscriptionMetricsConfig(
    @Valid
        SubscriptionMetricConfig<MessageProcessingDurationMetricOptions>
            messageProcessingDuration) {
  public static final SubscriptionMetricsConfig DISABLED =
      new SubscriptionMetricsConfig(SubscriptionMetricConfig.disabled());
}
