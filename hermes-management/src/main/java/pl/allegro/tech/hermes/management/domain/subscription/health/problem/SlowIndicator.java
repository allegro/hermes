package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;

import static pl.allegro.tech.hermes.api.SubscriptionHealth.Problem.SLOW;

public class SlowIndicator extends AbstractSubscriptionHealthProblemIndicator {
    private final double minSubscriptionToTopicSpeedRatio;

    public SlowIndicator(double minSubscriptionToTopicSpeedRatio) {
        this.minSubscriptionToTopicSpeedRatio = minSubscriptionToTopicSpeedRatio;
    }

    @Override
    public boolean problemOccurs(SubscriptionHealthContext context) {
        double subscriptionRate = context.getSubscriptionMetrics().getRate();
        double topicRate = context.getTopicMetrics().getRate();
        return subscriptionRate < minSubscriptionToTopicSpeedRatio * topicRate;
    }

    @Override
    public SubscriptionHealth.Problem getProblem() {
        return SLOW;
    }
}
