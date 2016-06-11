package pl.allegro.tech.hermes.frontend.cache.topic;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.Collection;
import java.util.Optional;

public interface TopicsCache {

    void start(Collection<? extends TopicCallback> callbacks);

    void stop();

    Optional<Topic> getTopic(TopicName topicName);
}
