package pl.allegro.tech.hermes.management.domain.subscription;

import pl.allegro.tech.hermes.api.TopicName;

public interface SubscriptionLagSource {
    long getLag(TopicName topicName, String subscriptionName);
}
