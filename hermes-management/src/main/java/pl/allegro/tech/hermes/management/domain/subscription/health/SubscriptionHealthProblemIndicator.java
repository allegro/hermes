package pl.allegro.tech.hermes.management.domain.subscription.health;

import pl.allegro.tech.hermes.api.SubscriptionHealth;

import java.util.Optional;

public interface SubscriptionHealthProblemIndicator {
    Optional<SubscriptionHealth.Problem> getProblemIfPresent(SubscriptionHealthContext context);
}
