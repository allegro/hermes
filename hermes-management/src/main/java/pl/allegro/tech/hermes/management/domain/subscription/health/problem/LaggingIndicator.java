package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;

import java.util.Optional;

import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.lagging;

public class LaggingIndicator implements SubscriptionHealthProblemIndicator {
    private final int maxLagInSeconds;

    public LaggingIndicator(int maxLagInSeconds) {
        this.maxLagInSeconds = maxLagInSeconds;
    }

    @Override
    public Optional<SubscriptionHealthProblem> getProblem(SubscriptionHealthContext context) {
        long subscriptionLag = context.getSubscriptionMetrics().getLag();
        double topicRate = context.getTopicMetrics().getRate();
        double lagInSeconds = subscriptionLag / topicRate;
        if (lagInSeconds > maxLagInSeconds) {
            return Optional.of(lagging(subscriptionLag));
        }
        return Optional.empty();
    }
}
