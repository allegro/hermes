package pl.allegro.tech.hermes.message.tracker.consumers;

import pl.allegro.tech.hermes.api.Subscription;

public class Trackers {

    private final SendingMessageTracker sendingMessageTracker;
    private final NoOperationSendingTracker noOperationDeliveryTracker;

    public Trackers(SendingMessageTracker sendingMessageTracker, NoOperationSendingTracker noOperationDeliveryTracker) {
        this.sendingMessageTracker = sendingMessageTracker;
        this.noOperationDeliveryTracker = noOperationDeliveryTracker;
    }

    public SendingTracker get(Subscription subscription) {
        return subscription.isTrackingEnabled() ? sendingMessageTracker : noOperationDeliveryTracker;
    }

}
