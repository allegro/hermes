package pl.allegro.tech.hermes.tracker.consumers;

import java.time.Clock;
import java.util.List;

public class DiscardedSendingTracker implements SendingTracker {

  private final List<LogRepository> repositories;
  private final Clock clock;

  DiscardedSendingTracker(List<LogRepository> repositories, Clock clock) {
    this.repositories = repositories;
    this.clock = clock;
  }

  @Override
  public void logSent(MessageMetadata message, String hostname) {}

  @Override
  public void logFailed(MessageMetadata message, String reason, String hostname) {}

  @Override
  public void logDiscarded(MessageMetadata message, String reason) {
    repositories.forEach(r -> r.logDiscarded(message, clock.millis(), reason));
  }

  @Override
  public void logInflight(MessageMetadata message) {}

  @Override
  public void logFiltered(MessageMetadata messageMetadata, String reason) {}
}
