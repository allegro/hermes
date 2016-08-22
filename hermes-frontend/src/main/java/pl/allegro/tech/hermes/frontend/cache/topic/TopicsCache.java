package pl.allegro.tech.hermes.frontend.cache.topic;

import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

import java.util.Optional;

public interface TopicsCache {

    Optional<CachedTopic> getTopic(String qualifiedTopicName);

    void start();

}
