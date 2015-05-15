package pl.allegro.tech.hermes.frontend.message.tracker;

import pl.allegro.tech.hermes.api.Topic;

import javax.inject.Inject;

public class Trackers {

    private final PublishingMessageTracker publishingMessageTracker;
    private final NoOperationPublishingTracker noOperationPublishingTracker;

    @Inject
    public Trackers(PublishingMessageTracker publishingMessageTracker, NoOperationPublishingTracker noOperationPublishingTracker) {
        this.publishingMessageTracker = publishingMessageTracker;
        this.noOperationPublishingTracker = noOperationPublishingTracker;
    }

    public PublishingTracker get(Topic topic) {
        return topic.isTrackingEnabled() ? publishingMessageTracker : noOperationPublishingTracker;
    }

}
