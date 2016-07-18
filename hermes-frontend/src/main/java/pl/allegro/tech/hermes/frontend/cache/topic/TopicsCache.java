package pl.allegro.tech.hermes.frontend.cache.topic;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.Collection;
import java.util.Optional;

public interface TopicsCache {

    Optional<Topic> getTopic(String topicName);

    void start();

}
