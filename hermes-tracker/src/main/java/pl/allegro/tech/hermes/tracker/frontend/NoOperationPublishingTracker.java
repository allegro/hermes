package pl.allegro.tech.hermes.tracker.frontend;

import java.util.Map;
import pl.allegro.tech.hermes.api.TopicName;

public class NoOperationPublishingTracker implements PublishingTracker {

  @Override
  public void logInflight(
      String messageId,
      TopicName topicName,
      String hostname,
      Map<String, String> extraRequestHeaders) {}

  @Override
  public void logPublished(
      String messageId,
      TopicName topicName,
      String hostname,
      String storageDatacenter,
      Map<String, String> extraRequestHeaders) {}

  @Override
  public void logError(
      String messageId,
      TopicName topicName,
      String reason,
      String hostname,
      Map<String, String> extraRequestHeaders) {}
}
