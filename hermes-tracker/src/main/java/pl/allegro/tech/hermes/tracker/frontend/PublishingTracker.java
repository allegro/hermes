package pl.allegro.tech.hermes.tracker.frontend;

import java.util.Map;
import pl.allegro.tech.hermes.api.TopicName;

public interface PublishingTracker {
  void logInflight(
      String messageId,
      TopicName topicName,
      String hostname,
      Map<String, String> extraRequestHeaders);

  void logPublished(
      String messageId,
      TopicName topicName,
      String hostname,
      String storageDatacenter,
      Map<String, String> extraRequestHeaders);

  void logError(
      String messageId,
      TopicName topicName,
      String reason,
      String hostname,
      Map<String, String> extraRequestHeaders);
}
