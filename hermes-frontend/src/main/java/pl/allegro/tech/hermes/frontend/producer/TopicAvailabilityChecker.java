package pl.allegro.tech.hermes.frontend.producer;

import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

public interface TopicAvailabilityChecker {

    boolean isTopicAvailable(CachedTopic topic);
}
