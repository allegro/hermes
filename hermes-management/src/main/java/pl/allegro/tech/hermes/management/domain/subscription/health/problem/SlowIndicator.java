package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;

import java.util.Optional;

import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.slow;

public class SlowIndicator implements SubscriptionHealthProblemIndicator {
    private final double minSubscriptionToTopicSpeedRatio;

    public SlowIndicator(double minSubscriptionToTopicSpeedRatio) {
        this.minSubscriptionToTopicSpeedRatio = minSubscriptionToTopicSpeedRatio;
    }

    @Override
    public Optional<SubscriptionHealthProblem> getProblemIfPresent(SubscriptionHealthContext context) {
        double subscriptionRate = context.getSubscriptionMetrics().getRate();
        double topicRate = context.getTopicMetrics().getRate();
        if (subscriptionRate < minSubscriptionToTopicSpeedRatio * topicRate) {
            return Optional.of(slow(subscriptionRate, topicRate));
        }
        return Optional.empty();
    }
}
