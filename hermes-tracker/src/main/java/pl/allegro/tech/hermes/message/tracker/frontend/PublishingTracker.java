package pl.allegro.tech.hermes.message.tracker.frontend;

import pl.allegro.tech.hermes.api.TopicName;

public interface PublishingTracker {
    void logInflight(String messageId, TopicName topicName);
    void logPublished(String messageId, TopicName topicName);
    void logError(String messageId, TopicName topicName, String reason);
}
