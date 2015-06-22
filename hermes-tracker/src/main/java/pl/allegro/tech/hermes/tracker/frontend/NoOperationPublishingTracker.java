package pl.allegro.tech.hermes.tracker.frontend;

import pl.allegro.tech.hermes.api.TopicName;

public class NoOperationPublishingTracker implements PublishingTracker {

    @Override
    public void logInflight(String messageId, TopicName topicName) {
    }

    @Override
    public void logPublished(String messageId, TopicName topicName) {
    }

    @Override
    public void logError(String messageId, TopicName topicName, String reason) {
    }
}
