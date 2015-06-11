package pl.allegro.tech.hermes.message.tracker.consumers;

import pl.allegro.tech.hermes.api.Subscription;

public class NoOperationSendingTracker implements SendingTracker {

    @Override
    public void logSent(MessageMetadata message, Subscription subscription) {

    }

    @Override
    public void logFailed(MessageMetadata message, Subscription subscription, String reason) {

    }

    @Override
    public void logDiscarded(MessageMetadata message, Subscription subscription, String reason) {

    }

    @Override
    public void logInflight(MessageMetadata message, Subscription subscription) {

    }
}
