package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.lagging;

import java.util.Optional;
import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;

public class LaggingIndicator implements SubscriptionHealthProblemIndicator {
  private final int maxLagInSeconds;

  public LaggingIndicator(int maxLagInSeconds) {
    this.maxLagInSeconds = maxLagInSeconds;
  }

  @Override
  public Optional<SubscriptionHealthProblem> getProblem(SubscriptionHealthContext context) {
    long subscriptionLag = context.getLag();
    double topicRate = context.getTopicRate();
    if (topicRate > 0.0 && subscriptionLag > maxLagInSeconds * topicRate) {
      return Optional.of(
          lagging(subscriptionLag, context.getSubscription().getQualifiedName().toString()));
    }
    return Optional.empty();
  }
}
