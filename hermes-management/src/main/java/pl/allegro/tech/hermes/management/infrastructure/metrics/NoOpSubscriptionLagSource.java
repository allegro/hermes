package pl.allegro.tech.hermes.management.infrastructure.metrics;

import pl.allegro.tech.hermes.api.MetricLongValue;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionLagSource;

public class NoOpSubscriptionLagSource implements SubscriptionLagSource {
  @Override
  public MetricLongValue getLag(TopicName topicName, String subscriptionName) {
    return MetricLongValue.of(-1);
  }
}
