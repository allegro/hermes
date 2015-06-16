package pl.allegro.tech.hermes.message.tracker.consumers;

public interface SendingTracker {
    void logSent(MessageMetadata message);
    void logFailed(MessageMetadata message, String reason);
    void logDiscarded(MessageMetadata message, String reason);
    void logInflight(MessageMetadata message);
}
