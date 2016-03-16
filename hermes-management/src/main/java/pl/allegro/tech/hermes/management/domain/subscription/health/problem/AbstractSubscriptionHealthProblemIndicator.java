package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;

import java.util.Optional;

abstract class AbstractSubscriptionHealthProblemIndicator implements SubscriptionHealthProblemIndicator {
    @Override
    public Optional<SubscriptionHealth.Problem> getProblemIfPresent(SubscriptionHealthContext context) {
        if (problemOccurs(context)) {
            return Optional.of(getProblem());
        } else {
            return Optional.empty();
        }
    }

    abstract boolean problemOccurs(SubscriptionHealthContext context);

    abstract SubscriptionHealth.Problem getProblem();
}
