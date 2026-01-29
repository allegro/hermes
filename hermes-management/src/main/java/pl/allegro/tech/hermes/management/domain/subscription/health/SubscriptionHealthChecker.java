package pl.allegro.tech.hermes.management.domain.subscription.health;

import static java.util.stream.Collectors.toSet;
import static pl.allegro.tech.hermes.api.Subscription.State.SUSPENDED;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.TopicMetrics;

public class SubscriptionHealthChecker {
  private final Set<SubscriptionHealthProblemIndicator> problemIndicators;

  public SubscriptionHealthChecker(List<SubscriptionHealthProblemIndicator> problemIndicators) {
    this.problemIndicators = Set.copyOf(problemIndicators);
  }

  public SubscriptionHealth checkHealth(
      Subscription subscription,
      TopicMetrics topicMetrics,
      SubscriptionMetrics subscriptionMetrics) {
    if (isSuspended(subscription)) {
      return SubscriptionHealth.HEALTHY;
    } else {
      return getActiveSubscriptionHealth(subscription, topicMetrics, subscriptionMetrics);
    }
  }

  private boolean isSuspended(Subscription subscription) {
    return subscription.getState() == SUSPENDED;
  }

  private SubscriptionHealth getActiveSubscriptionHealth(
      Subscription subscription,
      TopicMetrics topicMetrics,
      SubscriptionMetrics subscriptionMetrics) {
    return SubscriptionHealthContext.createIfAllMetricsExist(
            subscription, topicMetrics, subscriptionMetrics)
        .map(
            healthContext -> {
              Set<SubscriptionHealthProblem> healthProblems = getHealthProblems(healthContext);
              return SubscriptionHealth.of(healthProblems);
            })
        .orElse(SubscriptionHealth.NO_DATA);
  }

  private Set<SubscriptionHealthProblem> getHealthProblems(
      SubscriptionHealthContext healthContext) {
    return problemIndicators.stream()
        .map(indicator -> indicator.getProblem(healthContext))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toSet());
  }
}
