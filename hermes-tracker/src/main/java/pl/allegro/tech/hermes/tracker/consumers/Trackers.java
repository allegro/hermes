package pl.allegro.tech.hermes.tracker.consumers;

import java.time.Clock;
import java.util.List;
import pl.allegro.tech.hermes.api.Subscription;

public class Trackers {

  private final List<LogRepository> repositories;
  private final SendingMessageTracker sendingMessageTracker;
  private final DiscardedSendingTracker discardedSendingTracker;
  private final NoOperationSendingTracker noOperationDeliveryTracker;

  public Trackers(List<LogRepository> repositories) {
    this.repositories = repositories;
    this.sendingMessageTracker = new SendingMessageTracker(repositories, Clock.systemUTC());
    this.discardedSendingTracker = new DiscardedSendingTracker(repositories, Clock.systemUTC());
    this.noOperationDeliveryTracker = new NoOperationSendingTracker();
  }

  public SendingTracker get(Subscription subscription) {
    switch (subscription.getTrackingMode()) {
      case TRACK_ALL:
        return sendingMessageTracker;
      case TRACK_DISCARDED_ONLY:
        return discardedSendingTracker;
      default:
        return noOperationDeliveryTracker;
    }
  }

  public void close() {
    repositories.forEach(LogRepository::close);
  }
}
