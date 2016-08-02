package pl.allegro.tech.hermes.consumers.supervisor.process;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Optional;

public class Signal {

    private final SignalType type;

    private final SubscriptionName target;

    private final Optional<Object> payload;

    public enum SignalType {
        START, RESTART, STOP, RETRANSMIT, UPDATE_SUBSCRIPTION, UPDATE_TOPIC, KILL_RESTART, CLEANUP
    }

    private Signal(SignalType type, SubscriptionName target, Optional<Object> payload) {
        this.type = type;
        this.target = target;
        this.payload = payload;
    }

    public static Signal of(SignalType type, SubscriptionName target, Object payload) {
        return new Signal(type, target, Optional.of(payload));
    }

    public static Signal of(SignalType type, SubscriptionName target) {
        return new Signal(type, target, Optional.empty());
    }

    public SignalType getType() {
        return type;
    }

    public SubscriptionName getTarget() {
        return target;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getPayload() {
        return (Optional<T>) payload;
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtractedPayload() {
        return (T) payload.get();
    }

    @Override
    public String toString() {
        return "Signal{" +
                "type=" + type +
                ", target=" + target +
                '}';
    }
}
