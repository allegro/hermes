package pl.allegro.tech.hermes.consumers.consumer;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.status.Status;

import java.util.List;

import static pl.allegro.tech.hermes.consumers.consumer.status.Status.ShutdownCause.CONTROLLED;
import static pl.allegro.tech.hermes.consumers.consumer.status.Status.ShutdownCause.MODULE_SHUTDOWN;
import static pl.allegro.tech.hermes.consumers.consumer.status.Status.ShutdownCause.RESTART;
import static pl.allegro.tech.hermes.consumers.consumer.status.Status.ShutdownCause.RETRANSMISSION;

public interface Consumer extends Runnable {

    Subscription getSubscription();
    Status getStatus();
    List<PartitionOffset> getOffsetsToCommit();

    void signalStop(Status.ShutdownCause cause);

    default void signalStop() {
        signalStop(CONTROLLED);
    }

    default void signalRestart() {
        signalStop(RESTART);
    };

    default void signalRetransmit() {
        signalStop(RETRANSMISSION);
    };

    default void signalShutdown(){
        signalStop(MODULE_SHUTDOWN);
    }

    void signalUpdate(Subscription modifiedSubscription);

    void waitUntilStopped() throws InterruptedException;
    boolean isConsuming();

    default void setThreadName() {
        Thread.currentThread().setName("Consumer-" + getSubscription().getId());
    }

    default void unsetThreadName() {
        Thread.currentThread().setName("Released thread");
    }
}
