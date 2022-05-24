package pl.allegro.tech.hermes.tracker.frontend;

public interface LogRepository {

    void logPublished(String messageId, long timestamp, String topicName, String hostname);

    void logError(String messageId, long timestamp, String topicName, String reason, String hostname);

    void logInflight(String messageId, long timestamp, String topicName, String hostname);

    void close();
}
