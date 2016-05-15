package pl.allegro.tech.hermes.consumers.supervisor.background;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;
import static pl.allegro.tech.hermes.consumers.consumer.status.Status.ShutdownCause.MODULE_SHUTDOWN;

public class AssignedConsumers implements Iterable<Consumer> {
    private final ConcurrentHashMap<SubscriptionName, Consumer> consumers = new ConcurrentHashMap<>();

    @Override
    public Iterator<Consumer> iterator() {
        return consumers.values().iterator();
    }

    public void add(Consumer consumer) {
        consumers.putIfAbsent(consumer.getSubscription().toSubscriptionName(), consumer);
    }

    public void stop(SubscriptionName subscription) {
        ofNullable(consumers.get(subscription)).ifPresent(Consumer::signalStop);
    }

    public void update(Subscription subscription) {
        ofNullable(consumers.get(subscription.toSubscriptionName())).ifPresent(c -> c.signalUpdate(subscription));
    }

    public void restart(SubscriptionName subscription) {
        ofNullable(consumers.get(subscription)).ifPresent(Consumer::signalRestart);
    }

    public void retransmit(SubscriptionName subscription) {
        ofNullable(consumers.get(subscription)).ifPresent(Consumer::signalRetransmit);
    }
}
