package pl.allegro.tech.hermes.consumers.message.tracker;

import pl.allegro.tech.hermes.api.Subscription;

import javax.inject.Inject;

public class Trackers {

    private final SendingMessageTracker sendingMessageTracker;
    private final NoOperationSendingTracker noOperationDeliveryTracker;

    @Inject
    public Trackers(SendingMessageTracker sendingMessageTracker, NoOperationSendingTracker noOperationDeliveryTracker) {
        this.sendingMessageTracker = sendingMessageTracker;
        this.noOperationDeliveryTracker = noOperationDeliveryTracker;
    }

    public SendingTracker get(Subscription subscription) {
        return subscription.isTrackingEnabled() ? sendingMessageTracker : noOperationDeliveryTracker;
    }

}
