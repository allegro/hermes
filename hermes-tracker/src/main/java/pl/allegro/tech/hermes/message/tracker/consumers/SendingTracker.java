package pl.allegro.tech.hermes.message.tracker.consumers;

import pl.allegro.tech.hermes.api.Subscription;

public interface SendingTracker {
    void logSent(MessageMetadata message, Subscription subscription);
    void logFailed(MessageMetadata message, Subscription subscription, String reason);
    void logDiscarded(MessageMetadata message, Subscription subscription, String reason);
    void logInflight(MessageMetadata message, Subscription subscription);
}
