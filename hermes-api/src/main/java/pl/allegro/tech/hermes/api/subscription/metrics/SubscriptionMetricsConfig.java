package pl.allegro.tech.hermes.api.subscription.metrics;

public record SubscriptionMetricsConfig(MessageProcessingDurationMetricOptions messageProcessing) {

  public static final SubscriptionMetricsConfig DISABLED =
      new SubscriptionMetricsConfig(MessageProcessingDurationMetricOptions.DISABLED);
}
