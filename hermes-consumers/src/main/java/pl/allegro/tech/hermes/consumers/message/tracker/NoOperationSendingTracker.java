package pl.allegro.tech.hermes.consumers.message.tracker;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;

public class NoOperationSendingTracker implements SendingTracker {

    @Override
    public void logSent(Message message, Subscription subscription) {

    }

    @Override
    public void logFailed(Message message, Subscription subscription, String reason) {

    }

    @Override
    public void logDiscarded(Message message, Subscription subscription, String reason) {

    }

    @Override
    public void logInflight(Message message, Subscription subscription) {

    }
}
