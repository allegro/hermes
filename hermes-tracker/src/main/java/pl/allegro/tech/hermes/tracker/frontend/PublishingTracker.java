package pl.allegro.tech.hermes.tracker.frontend;

import pl.allegro.tech.hermes.api.TopicName;

public interface PublishingTracker {
    void logInflight(String messageId, TopicName topicName, String hostname);
    void logPublished(String messageId, TopicName topicName, String hostname);
    void logError(String messageId, TopicName topicName, String reason, String hostname);
}
