package pl.allegro.tech.hermes.frontend.cache.topic;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.frontend.metric.TopicWithMetrics;

import java.util.Collection;
import java.util.Optional;

public interface TopicsCache {
    void start(Collection<? extends TopicCallback> callbacks);
    void stop();

    Optional<TopicWithMetrics> getTopic(TopicName topicName);
}
