package pl.allegro.tech.hermes.tracker.consumers;

import java.time.Clock;
import java.util.List;

class SendingMessageTracker implements SendingTracker {
  private final List<LogRepository> repositories;
  private final Clock clock;

  SendingMessageTracker(List<LogRepository> repositories, Clock clock) {
    this.repositories = repositories;
    this.clock = clock;
  }

  @Override
  public void logSent(MessageMetadata message, String hostname) {
    repositories.forEach(r -> r.logSuccessful(message, hostname, clock.millis()));
  }

  @Override
  public void logFailed(MessageMetadata message, String reason, String hostname) {
    repositories.forEach(r -> r.logFailed(message, hostname, clock.millis(), reason));
  }

  @Override
  public void logDiscarded(MessageMetadata message, String reason) {
    repositories.forEach(r -> r.logDiscarded(message, clock.millis(), reason));
  }

  @Override
  public void logInflight(MessageMetadata message) {
    repositories.forEach(r -> r.logInflight(message, clock.millis()));
  }

  @Override
  public void logFiltered(MessageMetadata message, String reason) {
    repositories.forEach(r -> r.logFiltered(message, clock.millis(), reason));
  }
}
