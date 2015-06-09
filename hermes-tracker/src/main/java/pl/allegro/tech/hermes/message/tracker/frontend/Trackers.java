package pl.allegro.tech.hermes.message.tracker.frontend;

import pl.allegro.tech.hermes.api.Topic;

public class Trackers {

    private final PublishingMessageTracker publishingMessageTracker;
    private final NoOperationPublishingTracker noOperationPublishingTracker;

    public Trackers(PublishingMessageTracker publishingMessageTracker, NoOperationPublishingTracker noOperationPublishingTracker) {
        this.publishingMessageTracker = publishingMessageTracker;
        this.noOperationPublishingTracker = noOperationPublishingTracker;
    }

    public PublishingTracker get(Topic topic) {
        return topic.isTrackingEnabled() ? publishingMessageTracker : noOperationPublishingTracker;
    }

}
