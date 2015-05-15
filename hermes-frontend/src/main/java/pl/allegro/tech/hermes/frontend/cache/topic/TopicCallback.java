package pl.allegro.tech.hermes.frontend.cache.topic;

import pl.allegro.tech.hermes.api.Topic;

public interface TopicCallback {
    void onTopicCreated(Topic topic);
    void onTopicRemoved(Topic topic);
    void onTopicChanged(Topic topic);
}
