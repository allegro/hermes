package pl.allegro.tech.hermes.tracker.frontend;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import pl.allegro.tech.hermes.api.TopicName;

public class PublishingMessageTracker implements PublishingTracker {

  private final List<LogRepository> repositories;
  private final Clock clock;

  public PublishingMessageTracker(List<LogRepository> repositories, Clock clock) {
    this.repositories = repositories;
    this.clock = clock;
  }

  @Override
  public void logInflight(
      String messageId,
      TopicName topicName,
      String hostname,
      Map<String, String> extraRequestHeaders) {
    repositories.forEach(
        r ->
            r.logInflight(
                messageId,
                clock.millis(),
                topicName.qualifiedName(),
                hostname,
                extraRequestHeaders));
  }

  @Override
  public void logPublished(
      String messageId,
      TopicName topicName,
      String hostname,
      String storageDatacenter,
      Map<String, String> extraRequestHeaders) {
    repositories.forEach(
        r ->
            r.logPublished(
                messageId,
                clock.millis(),
                topicName.qualifiedName(),
                hostname,
                storageDatacenter,
                extraRequestHeaders));
  }

  @Override
  public void logError(
      String messageId,
      TopicName topicName,
      String reason,
      String hostname,
      Map<String, String> extraRequestHeaders) {
    repositories.forEach(
        r ->
            r.logError(
                messageId,
                clock.millis(),
                topicName.qualifiedName(),
                reason,
                hostname,
                extraRequestHeaders));
  }

  public void add(LogRepository logRepository) {
    repositories.add(logRepository);
  }

  public void close() {
    repositories.forEach(LogRepository::close);
  }
}
