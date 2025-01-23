package pl.allegro.tech.hermes.tracker.frontend;

import java.time.Clock;
import java.util.List;
import pl.allegro.tech.hermes.api.Topic;

public class Trackers {

  private final PublishingMessageTracker publishingMessageTracker;
  private final NoOperationPublishingTracker noOperationPublishingTracker;

  public Trackers(List<LogRepository> logRepositories) {
    this(
        new PublishingMessageTracker(logRepositories, Clock.systemUTC()),
        new NoOperationPublishingTracker());
  }

  Trackers(
      PublishingMessageTracker publishingMessageTracker,
      NoOperationPublishingTracker noOperationPublishingTracker) {
    this.publishingMessageTracker = publishingMessageTracker;
    this.noOperationPublishingTracker = noOperationPublishingTracker;
  }

  public PublishingTracker get(Topic topic) {
    return topic.isTrackingEnabled() ? publishingMessageTracker : noOperationPublishingTracker;
  }

  public void add(LogRepository logRepository) {
    publishingMessageTracker.add(logRepository);
  }

  public void close() {
    publishingMessageTracker.close();
  }
}
