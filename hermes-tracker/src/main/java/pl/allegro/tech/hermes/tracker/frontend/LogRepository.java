package pl.allegro.tech.hermes.tracker.frontend;

import java.util.Map;

public interface LogRepository {

  void logPublished(
      String messageId,
      long timestamp,
      String topicName,
      String hostname,
      String storageDatacenter,
      Map<String, String> extraRequestHeaders);

  void logError(
      String messageId,
      long timestamp,
      String topicName,
      String reason,
      String hostname,
      Map<String, String> extraRequestHeaders);

  void logInflight(
      String messageId,
      long timestamp,
      String topicName,
      String hostname,
      Map<String, String> extraRequestHeaders);

  void close();
}
