package pl.allegro.tech.hermes.management.domain.subscription.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.TopicMetrics;

import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static pl.allegro.tech.hermes.api.Subscription.State.SUSPENDED;

@Component
public class SubscriptionHealthChecker {
    private final Set<SubscriptionHealthProblemIndicator> problemIndicators;

    @Autowired
    public SubscriptionHealthChecker(Set<SubscriptionHealthProblemIndicator> problemIndicators) {
        this.problemIndicators = problemIndicators;
    }

    public SubscriptionHealth checkHealth(Subscription subscription, TopicMetrics topicMetrics, SubscriptionMetrics subscriptionMetrics) {
        if (isSuspended(subscription)) {
            return SubscriptionHealth.HEALTHY;
        } else {
            return getActiveSubscriptionHealth(subscription, topicMetrics, subscriptionMetrics);
        }
    }

    private boolean isSuspended(Subscription subscription) {
        return subscription.getState() == SUSPENDED;
    }

    private SubscriptionHealth getActiveSubscriptionHealth(Subscription subscription, TopicMetrics topicMetrics, SubscriptionMetrics subscriptionMetrics) {
        try {
            SubscriptionHealthContext healthContext = new SubscriptionHealthContext(subscription, topicMetrics, subscriptionMetrics);
            Set<SubscriptionHealthProblem> healthProblems = getHealthProblems(healthContext);
            return SubscriptionHealth.of(healthProblems);
        } catch (NumberFormatException e) {
            return SubscriptionHealth.NO_DATA;
        }
    }

    private Set<SubscriptionHealthProblem> getHealthProblems(SubscriptionHealthContext healthContext) {
        return problemIndicators.stream()
                .map(indicator -> indicator.getProblem(healthContext))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toSet());
    }
}
