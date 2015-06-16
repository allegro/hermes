package pl.allegro.tech.hermes.message.tracker.consumers;


public interface LogRepository {

    void logSuccessful(MessageMetadata message, long timestamp);
    void logFailed(MessageMetadata message, long timestamp, String reason);
    void logDiscarded(MessageMetadata message, long timestamp, String reason);
    void logInflight(MessageMetadata message, long timestamp);

}
