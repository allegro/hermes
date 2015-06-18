package pl.allegro.tech.hermes.tracker.frontend;

public interface LogRepository {

    void logPublished(String messageId, long timestamp, String topicName);

    void logError(String messageId, long timestamp, String topicName, String reason);

    void logInflight(String messageId, long timestamp, String topicName);

}
