package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;

import java.util.Optional;

public class DisabledIndicator implements SubscriptionHealthProblemIndicator {

    @Override
    public Optional<SubscriptionHealthProblem> getProblem(SubscriptionHealthContext context) {
        return Optional.empty();
    }
}
