package pl.allegro.tech.hermes.management.domain.subscription.health;

import java.util.Optional;
import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;

public interface SubscriptionHealthProblemIndicator {
  Optional<SubscriptionHealthProblem> getProblem(SubscriptionHealthContext context);
}
