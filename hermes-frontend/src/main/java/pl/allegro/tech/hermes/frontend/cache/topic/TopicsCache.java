package pl.allegro.tech.hermes.frontend.cache.topic;

import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

import java.util.List;
import java.util.Optional;

public interface TopicsCache {

    Optional<CachedTopic> getTopic(String qualifiedTopicName);

    List<CachedTopic> getTopics();

    void start();
}
