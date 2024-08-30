package pl.allegro.tech.hermes.tracker.consumers;

public interface SendingTracker {
  void logSent(MessageMetadata message, String hostname);

  void logFailed(MessageMetadata message, String reason, String hostname);

  void logDiscarded(MessageMetadata message, String reason);

  void logInflight(MessageMetadata message);

  void logFiltered(MessageMetadata messageMetadata, String reason);
}
