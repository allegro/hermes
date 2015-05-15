package pl.allegro.tech.hermes.consumers.message.tracker;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

public interface SendingTracker {
    void logSent(Message message, Subscription subscription);
    void logFailed(Message message, Subscription subscription, String reason);
    void logDiscarded(Message message, Subscription subscription, String reason);
    void logInflight(Message message, Subscription subscription);
}
