package pl.allegro.tech.hermes.tracker.consumers;

public interface SendingTracker {
    void logSent(MessageMetadata message);
    void logFailed(MessageMetadata message, String reason);
    void logDiscarded(MessageMetadata message, String reason);
    void logInflight(MessageMetadata message);
    void logFiltered(MessageMetadata messageMetadata, String reason);
}
