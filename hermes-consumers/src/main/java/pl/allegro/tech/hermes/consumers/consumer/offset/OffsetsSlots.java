package pl.allegro.tech.hermes.consumers.consumer.offset;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class OffsetsSlots {

    private final ConcurrentHashMap<SubscriptionPartitionOffset, MessageState> slots = new ConcurrentHashMap<>();
    private final Semaphore inflightSemaphore = new Semaphore(60);
    private final Semaphore totalOffsetsCountSemaphore = new Semaphore(20_000);

    // used by sender thread
    public void markAsSent(SubscriptionPartitionOffset subscriptionPartitionOffset) {
        inflightSemaphore.release();
        slots.put(subscriptionPartitionOffset, MessageState.DELIVERED);
    }

    public boolean hasSpace() throws InterruptedException {
        int inflightPermits = inflightSemaphore.availablePermits();
        int totalPermits = totalOffsetsCountSemaphore.availablePermits();

        if (inflightPermits == 0 || totalPermits == 0) {
            Thread.sleep(100);
            return false;
        } else {
            return true;
        }
    }

//            inflightSemaphore.acquire();
//            deliveredSemaphore.acquire();

    // used by consumer thread
    public void addSlot(SubscriptionPartitionOffset subscriptionPartitionOffset) throws InterruptedException {
        totalOffsetsCountSemaphore.acquire();
        inflightSemaphore.acquire();
        slots.put(subscriptionPartitionOffset, MessageState.INFLIGHT);
    }


    // used by consumer thread
    // lists have to be sorted in ascending order
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


// inflight = [1, 2, 3, 4, 5] deliveredQueue = [........................................]
// inflight = [1, 2, 4] deliveredQueue = [3, 5] -> [], maxDelivered = 5
// inflight = [1, 2] deliveredQueue = [4] -> [], maxDelivered = 5
// inflight = [2] deliveredQueue = [1] -> [], maxDelivered = 5, min(2, maxDelivered) = 2

// jest kompresja deliveredOffsets, ale nie ma kompresji dla inflights




// OffsetSlots:      { [1, Del], [2, Inf], [3, Inf], [4, Del], [5, Del] }
// OffsetCommitter:  inflight = { [2, Inf], [3, Inf] }, delivered = { [1, Del], [4, Del], [5, Del] }

// OffsetSlots:      { [2, Inf], [3, Inf] }
// OffsetCommitter:  inflight = { [2, Inf], [3, Inf] }, maxDelivered = [5, Del]

// OffsetSlots:      { [2, Inf], [3, Del] }
// OffsetCommitter:  inflight = { [2, Inf] }, delivered = { [3, Del] }, maxDelivered = [5, Del]

