package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.timingOut;

import java.util.Optional;
import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;

public class TimingOutIndicator implements SubscriptionHealthProblemIndicator {
  private final double maxTimeoutsRatio;
  private final double minSubscriptionRateForReliableMetrics;

  public TimingOutIndicator(double maxTimeoutsRatio, double minSubscriptionRateForReliableMetrics) {
    this.maxTimeoutsRatio = maxTimeoutsRatio;
    this.minSubscriptionRateForReliableMetrics = minSubscriptionRateForReliableMetrics;
  }

  @Override
  public Optional<SubscriptionHealthProblem> getProblem(SubscriptionHealthContext context) {
    if (areSubscriptionMetricsReliable(context) && isTimeoutsRateHigh(context)) {
      return Optional.of(
          timingOut(
              context.getTimeoutsRate(), context.getSubscription().getQualifiedName().toString()));
    }
    return Optional.empty();
  }

  private boolean areSubscriptionMetricsReliable(SubscriptionHealthContext context) {
    return context.getSubscriptionRateRespectingDeliveryType()
        > minSubscriptionRateForReliableMetrics;
  }

  private boolean isTimeoutsRateHigh(SubscriptionHealthContext context) {
    double timeoutsRate = context.getTimeoutsRate();
    return timeoutsRate > maxTimeoutsRatio * context.getSubscriptionRateRespectingDeliveryType();
  }
}
