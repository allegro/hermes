package pl.allegro.tech.hermes.consumers.consumer.status;

import java.util.Optional;

import static java.util.Optional.empty;

public class Status {

    private final StatusType statusType;
    private final long activeFromTimestamp;
    private final Optional<ShutdownCause> shutdownCause;

    public Status(StatusType statusType, long activeFromTimestamp) {
        this.statusType = statusType;
        this.activeFromTimestamp = activeFromTimestamp;
        this.shutdownCause = empty();
    }

    public Status(StatusType statusType, ShutdownCause cause, long activeFromTimestamp) {
        this.statusType = statusType;
        this.activeFromTimestamp = activeFromTimestamp;
        this.shutdownCause = Optional.of(cause);
    }

    public StatusType getType() {
        return statusType;
    }

    public long getActiveFromTimestamp() {
        return activeFromTimestamp;
    }

    public Optional<ShutdownCause> getShutdownCause() {
        return shutdownCause;
    }

    public enum StatusType {
        NEW, STARTING, STARTED, CONSUMING, STOPPING, STOPPED
    }

    public enum ShutdownCause {
        BROKEN, RESTART, RETRANSMISSION, CONTROLLED, MODULE_SHUTDOWN
    }

    @Override
    public String toString() {
        return "[" +
                "statusType=" + statusType +
                (shutdownCause.isPresent()? "shutdownCause=" + shutdownCause.get() : "") +
                ", timestamp=" + activeFromTimestamp +
                ']';
    }
}
