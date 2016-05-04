package pl.allegro.tech.hermes.consumers.consumer.status;

import java.time.Clock;
import java.util.Optional;

import static pl.allegro.tech.hermes.consumers.consumer.status.Status.StatusType.*;

public class MutableStatus {
    private final Clock clock;
    private volatile Status status;

    public MutableStatus(Clock clock) {
        this.clock = clock;
        this.status = new Status(NEW, clock.millis());
    }

    public void set(Status.StatusType statusType) {
        this.status = new Status(statusType, clock.millis());
    }

    public void set(Status.StatusType statusType, Status.ShutdownCause cause) {
        this.status = new Status(statusType, cause, clock.millis());
    }

    public Status get() {
        return status;
    }

    public void advance(Status.StatusType statusType) {
        if (status.getShutdownCause().isPresent()) {
            set(statusType, status.getShutdownCause().get());
        } else {
            set(statusType);
        }
    }
}
