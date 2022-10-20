package pl.allegro.tech.hermes.tracker.frontend;

import pl.allegro.tech.hermes.api.TopicName;

import java.util.Map;

public interface PublishingTracker {
    void logInflight(String messageId, TopicName topicName, String hostname, Map<String, String> extraRequestHeaders);

    void logPublished(String messageId, TopicName topicName, String hostname, Map<String, String> extraRequestHeaders);

    void logError(String messageId, TopicName topicName, String reason, String hostname, Map<String, String> extraRequestHeaders);
}
