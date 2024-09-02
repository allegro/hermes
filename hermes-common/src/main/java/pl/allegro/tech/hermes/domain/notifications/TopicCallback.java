package pl.allegro.tech.hermes.domain.notifications;

import pl.allegro.tech.hermes.api.Topic;

public interface TopicCallback {

  default void onTopicCreated(Topic topic) {}

  default void onTopicRemoved(Topic topic) {}

  default void onTopicChanged(Topic topic) {}
}
