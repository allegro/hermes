package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import java.util.Optional;
import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;

public class DisabledIndicator implements SubscriptionHealthProblemIndicator {

  @Override
  public Optional<SubscriptionHealthProblem> getProblem(SubscriptionHealthContext context) {
    return Optional.empty();
  }
}
