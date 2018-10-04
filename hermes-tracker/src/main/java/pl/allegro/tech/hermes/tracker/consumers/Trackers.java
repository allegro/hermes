package pl.allegro.tech.hermes.tracker.consumers;

import pl.allegro.tech.hermes.api.Subscription;

import java.time.Clock;
import java.util.List;

public class Trackers {

    private final SendingMessageTracker sendingMessageTracker;
    private final DiscardedSendingTracker discardedSendingTracker;
    private final NoOperationSendingTracker noOperationDeliveryTracker;

    public Trackers(List<LogRepository> repositories) {
        this(new SendingMessageTracker(repositories, Clock.systemUTC()),
                new DiscardedSendingTracker(repositories, Clock.systemUTC()),
                new NoOperationSendingTracker());
    }

    Trackers(SendingMessageTracker sendingMessageTracker,
             DiscardedSendingTracker discardedSendingTracker,
             NoOperationSendingTracker noOperationDeliveryTracker) {

        this.sendingMessageTracker = sendingMessageTracker;
        this.discardedSendingTracker = discardedSendingTracker;
        this.noOperationDeliveryTracker = noOperationDeliveryTracker;
    }

    public SendingTracker get(Subscription subscription) {
        if (subscription.isFullTrackingEnabled()) {
            return sendingMessageTracker;
        } else if (subscription.isDiscardedTrackingEnabled()) {
            return discardedSendingTracker;
        }

        return noOperationDeliveryTracker;
    }

    public void add(LogRepository logRepository) {
        sendingMessageTracker.add(logRepository);
    }
}
