package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;

import static pl.allegro.tech.hermes.api.SubscriptionHealth.Problem.LAGGING;

public class LaggingIndicator extends AbstractSubscriptionHealthProblemIndicator {
    private final int maxLagInSeconds;

    public LaggingIndicator(int maxLagInSeconds) {
        this.maxLagInSeconds = maxLagInSeconds;
    }

    @Override
    public boolean problemOccurs(SubscriptionHealthContext context) {
        long subscriptionLag = context.getSubscriptionMetrics().getLag();
        double topicRate = context.getTopicMetrics().getRate();
        double lagInSeconds = subscriptionLag / topicRate;
        return lagInSeconds > maxLagInSeconds;
    }

    @Override
    public SubscriptionHealth.Problem getProblem() {
        return LAGGING;
    }
}
