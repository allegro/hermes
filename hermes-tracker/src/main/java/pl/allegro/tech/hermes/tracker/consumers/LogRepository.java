package pl.allegro.tech.hermes.tracker.consumers;

public interface LogRepository {

  void logSuccessful(MessageMetadata message, String hostname, long timestamp);

  void logFailed(MessageMetadata message, String hostname, long timestamp, String reason);

  void logDiscarded(MessageMetadata message, long timestamp, String reason);

  void logInflight(MessageMetadata message, long timestamp);

  void logFiltered(MessageMetadata message, long timestamp, String reason);

  void close();
}
