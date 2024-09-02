package pl.allegro.tech.hermes.management.domain.subscription;

import pl.allegro.tech.hermes.api.MetricLongValue;
import pl.allegro.tech.hermes.api.TopicName;

public interface SubscriptionLagSource {
  MetricLongValue getLag(TopicName topicName, String subscriptionName);
}
