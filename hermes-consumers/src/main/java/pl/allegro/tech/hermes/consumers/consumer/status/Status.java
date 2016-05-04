package pl.allegro.tech.hermes.consumers.consumer.status;

import java.util.Optional;

import static java.util.Optional.empty;

public class Status {
    private final StatusType statusType;
    private final long timestamp;
    private final Optional<ShutdownCause> shutdownCause;

    public Status(StatusType statusType, long timestamp) {
        this.statusType = statusType;
        this.timestamp = timestamp;
        this.shutdownCause = empty();
    }

    public Status(StatusType statusType, ShutdownCause cause, long timestamp) {
        this.statusType = statusType;
        this.timestamp = timestamp;
        this.shutdownCause = Optional.of(cause);
    }

    public StatusType getType() {
        return statusType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Optional<ShutdownCause> getShutdownCause() {
        return shutdownCause;
    }

    public static enum StatusType {
        NEW, STARTING, STARTED, CONSUMING, STOPPING, STOPPED
    }

    public static enum ShutdownCause {
        BROKEN, RESTART, RETRANSMISSION, CONTROLLED, MODULE_SHUTDOWN
    }

    @Override
    public String toString() {
        return "[" +
                "statusType=" + statusType +
                (shutdownCause.isPresent()? "shutdownCause=" + shutdownCause.get() : "") +
                ", timestamp=" + timestamp +
                ']';
    }
}
