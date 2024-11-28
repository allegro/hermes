package pl.allegro.tech.hermes.test.helper.environment;

import pl.allegro.tech.hermes.api.Topic;

public interface FrontendNotification {
  void notifyTopicCreated(Topic topic);

  void notifyTopicBlacklisted(Topic topic);

  void notifyTopicUnblacklisted(Topic topic);
}
