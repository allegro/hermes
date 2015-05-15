package pl.allegro.tech.hermes.management.domain.subscription;

import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.TopicName;

public interface SubscriptionMetricsRepository {

    SubscriptionMetrics loadMetrics(TopicName topicName, String subscriptionName);

}
