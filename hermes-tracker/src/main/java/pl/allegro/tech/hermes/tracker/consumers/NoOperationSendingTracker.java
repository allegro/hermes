package pl.allegro.tech.hermes.tracker.consumers;

public class NoOperationSendingTracker implements SendingTracker {

  @Override
  public void logSent(MessageMetadata message, String hostname) {}

  @Override
  public void logFailed(MessageMetadata message, String reason, String hostname) {}

  @Override
  public void logDiscarded(MessageMetadata message, String reason) {}

  @Override
  public void logInflight(MessageMetadata message) {}

  @Override
  public void logFiltered(MessageMetadata messageMetadata, String reason) {}
}
