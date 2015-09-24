package pl.allegro.tech.hermes.management.infrastructure.metrics;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionLagSource;

public class NoOpSubscriptionLagSource implements SubscriptionLagSource {
    @Override
    public long getLag(TopicName topicName, String subscriptionName) {
        return -1;
    }
}
