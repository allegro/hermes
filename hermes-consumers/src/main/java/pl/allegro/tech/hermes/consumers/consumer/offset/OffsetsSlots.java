package pl.allegro.tech.hermes.consumers.consumer.offset;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.rate.AdjustableSemaphore;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class OffsetsSlots {

    private final ConcurrentHashMap<SubscriptionPartitionOffset, MessageState> slots = new ConcurrentHashMap<>();
    private final AdjustableSemaphore inflightSemaphore;
    private final Semaphore totalOffsetsCountSemaphore;

    public OffsetsSlots(SubscriptionName subscriptionName, MetricsFacade metrics, int offsetQueueSize, int inflightQueueSize) {
        this.totalOffsetsCountSemaphore = new Semaphore(offsetQueueSize);
        this.inflightSemaphore = new AdjustableSemaphore(inflightQueueSize);
        metrics.subscriptions().registerOffsetsQueueGauge(subscriptionName, totalOffsetsCountSemaphore,  slots -> (double) slots.availablePermits() / offsetQueueSize);
    }

    public void setInflightSize(int inflightQueueSize) {
        this.inflightSemaphore.setMaxPermits(inflightQueueSize);
    }

    /**
     * This method is used by message sender
     */
    public void markAsSent(SubscriptionPartitionOffset subscriptionPartitionOffset) {
        inflightSemaphore.release();
        slots.put(subscriptionPartitionOffset, MessageState.DELIVERED);
    }

    /**
     * This method is used by consumer
     */
    public boolean hasSpace(Duration processingInterval) throws InterruptedException {
        int inflightPermits = inflightSemaphore.availablePermits();
        int totalPermits = totalOffsetsCountSemaphore.availablePermits();

        if (inflightPermits == 0 || totalPermits == 0) {
            Thread.sleep(processingInterval.toMillis());
            return false;
        } else {
            return true;
        }
    }

    /**
     * This method is used by consumer
     */
    public void addSlot(SubscriptionPartitionOffset subscriptionPartitionOffset) throws InterruptedException {
        totalOffsetsCountSemaphore.acquire();
        inflightSemaphore.acquire();
        slots.put(subscriptionPartitionOffset, MessageState.INFLIGHT);
    }

    /**
     * This method is used by consumer
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
}
