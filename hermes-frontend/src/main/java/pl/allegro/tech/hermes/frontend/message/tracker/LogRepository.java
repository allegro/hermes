package pl.allegro.tech.hermes.frontend.message.tracker;

public interface LogRepository {

    void logPublished(String messageId, long timestamp, String topicName);

    void logError(String messageId, long timestamp, String topicName, String reason);

    void logInflight(String messageId, long timestamp, String topicName);

}
