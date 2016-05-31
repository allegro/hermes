package pl.allegro.tech.hermes.consumers.supervisor.background;

import java.util.Optional;

public class Signal {

    private final SignalType type;

    private final Optional<Object> payload;

    public enum SignalType {
        RESTART, STOP, RETRANSMIT, UPDATE
    }

    private Signal(SignalType type, Optional<Object> payload) {
        this.type = type;
        this.payload = payload;
    }

    public static Signal of(SignalType type, Object payload) {
        return new Signal(type, Optional.of(payload));
    }

    public static Signal of(SignalType type) {
        return new Signal(type, Optional.empty());
    }

    public SignalType getType() {
        return type;
    }

    public Optional<Object> getPayload() {
        return payload;
    }
}
