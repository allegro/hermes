package pl.allegro.tech.hermes.management.domain.subscription.health;

import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;

import java.util.Optional;

public interface SubscriptionHealthProblemIndicator {
    Optional<SubscriptionHealthProblem> getProblem(SubscriptionHealthContext context);
}
