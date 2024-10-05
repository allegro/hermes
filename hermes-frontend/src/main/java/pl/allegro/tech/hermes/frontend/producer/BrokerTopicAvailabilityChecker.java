package pl.allegro.tech.hermes.frontend.producer;

import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

public interface BrokerTopicAvailabilityChecker {

  boolean areAllTopicsAvailable();

  boolean isTopicAvailable(CachedTopic cachedTopic);
}
