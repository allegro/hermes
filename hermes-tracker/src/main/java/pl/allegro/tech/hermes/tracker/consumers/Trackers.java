package pl.allegro.tech.hermes.tracker.consumers;

import pl.allegro.tech.hermes.api.Subscription;

import java.time.Clock;
import java.util.List;

public class Trackers {

    private final SendingMessageTracker sendingMessageTracker;
    private final NoOperationSendingTracker noOperationDeliveryTracker;

    public Trackers(List<LogRepository> repositories) {
        this(new SendingMessageTracker(repositories, Clock.systemUTC()), new NoOperationSendingTracker());
    }

    Trackers(SendingMessageTracker sendingMessageTracker, NoOperationSendingTracker noOperationDeliveryTracker) {
        this.sendingMessageTracker = sendingMessageTracker;
        this.noOperationDeliveryTracker = noOperationDeliveryTracker;
    }

    public SendingTracker get(Subscription subscription) {
        return subscription.isTrackingEnabled() ? sendingMessageTracker : noOperationDeliveryTracker;
    }

    public void add(LogRepository logRepository) {
        sendingMessageTracker.add(logRepository);
    }
}
