package pl.allegro.tech.hermes.tracker.frontend;

import pl.allegro.tech.hermes.api.Topic;

import java.time.Clock;
import java.util.List;

public class Trackers {

    private final PublishingMessageTracker publishingMessageTracker;
    private final ErrorPublishingMessageTracker errorPublishingMessageTracker;
    private final NoOperationPublishingTracker noOperationPublishingTracker;

    public Trackers(List<LogRepository> logRepositories) {
        this(new PublishingMessageTracker(logRepositories, Clock.systemUTC()),
                new ErrorPublishingMessageTracker(logRepositories, Clock.systemUTC()),
                new NoOperationPublishingTracker());
    }

    Trackers(PublishingMessageTracker publishingMessageTracker,
             ErrorPublishingMessageTracker errorPublishingMessageTracker,
             NoOperationPublishingTracker noOperationPublishingTracker) {
        this.publishingMessageTracker = publishingMessageTracker;
        this.errorPublishingMessageTracker = errorPublishingMessageTracker;
        this.noOperationPublishingTracker = noOperationPublishingTracker;
    }

    public PublishingTracker get(Topic topic) {

        if(topic.isFullTrackingEnabled()) {
            return publishingMessageTracker;
        } else if(topic.isErrorTrackingEnabled()) {
            return errorPublishingMessageTracker;
        }

        return noOperationPublishingTracker;
    }

    public void add(LogRepository logRepository) {
        publishingMessageTracker.add(logRepository);
    }
}
