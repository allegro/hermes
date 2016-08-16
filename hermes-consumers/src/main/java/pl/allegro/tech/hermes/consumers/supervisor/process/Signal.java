package pl.allegro.tech.hermes.consumers.supervisor.process;

import pl.allegro.tech.hermes.api.SubscriptionName;

public class Signal {

    private final SignalType type;

    private final SubscriptionName target;

    private final Object payload;

    private final long executeAfterTimestamp;

    public enum SignalType {
        START, STOP, RETRANSMIT, UPDATE_SUBSCRIPTION, UPDATE_TOPIC, KILL,
        RESTART, RESTART_UNHEALTHY, STOP_RESTART, KILL_UNHEALTHY, CLEANUP
    }

    private Signal(SignalType type, SubscriptionName target, Object payload, long executeAfterTimestamp) {
        this.type = type;
        this.target = target;
        this.payload = payload;
        this.executeAfterTimestamp = executeAfterTimestamp;
    }

    public static Signal of(SignalType type, SubscriptionName target, Object payload) {
        return new Signal(type, target, payload, -1);
    }

    public static Signal of(SignalType type, SubscriptionName target) {
        return new Signal(type, target, null, -1);
    }

    public static Signal of(SignalType type, SubscriptionName target, long executeAfterTimestamp) {
        return new Signal(type, target, null, executeAfterTimestamp);
    }

    SignalType getType() {
        return type;
    }

    SubscriptionName getTarget() {
        return target;
    }

    boolean canExecuteNow(long currentTimestamp) {
        return currentTimestamp > executeAfterTimestamp;
    }

    @SuppressWarnings("unchecked")
    <T> T getPayload() {
        return (T) payload;
    }

    @Override
    public String toString() {
        return "Signal{" +
                "type=" + type +
                ", target=" + target +
                '}';
    }
}
