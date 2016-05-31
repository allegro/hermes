package pl.allegro.tech.hermes.consumers.supervisor.background;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class AssignedConsumers {

    private final ConcurrentMap<SubscriptionName, ConsumerProcess> consumers = new ConcurrentHashMap<>();

    public Stream<Map.Entry<SubscriptionName, ConsumerProcess>> stream() {
        return consumers.entrySet().stream();
    }

    public void add(SubscriptionName subscriptionName, ConsumerProcess process) {
        consumers.putIfAbsent(subscriptionName, process);
    }

    public void stop(SubscriptionName subscription) {
        sendSignal(subscription, Signal.of(Signal.SignalType.STOP));
    }

    public void update(Subscription subscription) {
        sendSignal(subscription.toSubscriptionName(), Signal.of(Signal.SignalType.UPDATE, subscription));
    }

    public void restart(SubscriptionName subscription) {
        sendSignal(subscription, Signal.of(Signal.SignalType.RESTART));
    }

    public void retransmit(SubscriptionName subscription) {
        sendSignal(subscription, Signal.of(Signal.SignalType.RETRANSMIT));
    }

    private void sendSignal(SubscriptionName subscriptionName, Signal signal) {
        consumers.computeIfPresent(subscriptionName, (k, p) -> p.accept(signal));
    }
}
