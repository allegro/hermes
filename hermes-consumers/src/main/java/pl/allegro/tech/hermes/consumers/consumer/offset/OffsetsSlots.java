package pl.allegro.tech.hermes.consumers.consumer.offset;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.rate.AdjustableSemaphore;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class OffsetsSlots {

    private final ConcurrentHashMap<SubscriptionPartitionOffset, MessageState> slots = new ConcurrentHashMap<>();
    private final AdjustableSemaphore inflightSemaphore;
    private final Semaphore totalOffsetsCountSemaphore;

    public OffsetsSlots(SubscriptionName subscriptionName, MetricsFacade metrics, int inflightQueueSize, int offsetQueueSize) {
        this.totalOffsetsCountSemaphore = new Semaphore(offsetQueueSize);
        this.inflightSemaphore = new AdjustableSemaphore(inflightQueueSize);
        metrics.subscriptions().registerOffsetsQueueGauge(subscriptionName, totalOffsetsCountSemaphore, slots -> (offsetQueueSize - (double) slots.availablePermits()) / offsetQueueSize);
    }

    public void setInflightSize(int inflightQueueSize) {
        this.inflightSemaphore.setMaxPermits(inflightQueueSize);
    }

    /**
     * This method is used by message sender.
     */
    public void markAsSent(SubscriptionPartitionOffset subscriptionPartitionOffset) {
        inflightSemaphore.release();
        slots.put(subscriptionPartitionOffset, MessageState.DELIVERED);
    }

    /**
     * This method is used by consumer.
     */
    public boolean tryToAcquireSlot(Duration processingInterval) throws InterruptedException {
        if (inflightSemaphore.tryAcquire(processingInterval.toMillis(), TimeUnit.MILLISECONDS)) {
            if (totalOffsetsCountSemaphore.tryAcquire(processingInterval.toMillis(), TimeUnit.MILLISECONDS)) {
                return true;
            }
            inflightSemaphore.release();
        }
        return false;
    }

    /**
     * This method is used by consumer.
     */
    public void addSlot(SubscriptionPartitionOffset subscriptionPartitionOffset) {
        slots.put(subscriptionPartitionOffset, MessageState.INFLIGHT);
    }

    /**
     * This method is used by consumer.
     */
    public Map<SubscriptionPartitionOffset, MessageState> offsetSnapshot() {
        int permitsReleased = 0;
        Map<SubscriptionPartitionOffset, MessageState> offsetSnapshot = new HashMap<>();

        for (Map.Entry<SubscriptionPartitionOffset, MessageState> entry : slots.entrySet()) {
            offsetSnapshot.put(entry.getKey(), entry.getValue());
            if (entry.getValue() == MessageState.DELIVERED) {
                slots.remove(entry.getKey());
                permitsReleased++;
            }
        }
        totalOffsetsCountSemaphore.release(permitsReleased);
        return offsetSnapshot;
    }

    /**
     * This method is used by consumer. It releases semaphores, when there is no message.
     */
    public void releaseAcquiredSlots() {
        inflightSemaphore.release();
        totalOffsetsCountSemaphore.release();
    }
}
