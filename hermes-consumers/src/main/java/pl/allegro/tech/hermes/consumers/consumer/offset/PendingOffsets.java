package pl.allegro.tech.hermes.consumers.consumer.offset;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.rate.AdjustableSemaphore;

/**
 * This class manages pending offsets for message consumption in a thread-safe manner. It ensures
 * that the number of pending offsets does not exceed a specified maximum limit.
 *
 * <p>The {@code slots} map is effectively a bounded map, guarded by the {@code
 * maxPendingOffsetsSemaphore}. This semaphore ensures that the number of entries in the {@code
 * slots} map does not exceed {@code maxPendingOffsets} and prevents running out of memory. The
 * semaphore is used to acquire permits before adding entries to the map and to release permits when
 * entries are removed.
 *
 * <p>The {@code inflightSemaphore} is used to limit the number of messages that are currently being
 * processed (inflight). It helps control the concurrency level of message processing.
 *
 * <p>Note: Methods that modify the state of the {@code slots} map, such as {@code markAsProcessed}
 * and {@code markAsInflight}, must only be called after successfully acquiring a permit using the
 * {@code tryAcquireSlot} method.
 */
public class PendingOffsets {

  private final ConcurrentHashMap<SubscriptionPartitionOffset, MessageState> slots =
      new ConcurrentHashMap<>();
  private final AdjustableSemaphore inflightSemaphore;
  private final Semaphore maxPendingOffsetsSemaphore;

  public PendingOffsets(
      SubscriptionName subscriptionName,
      MetricsFacade metrics,
      int inflightQueueSize,
      int maxPendingOffsets) {
    this.maxPendingOffsetsSemaphore = new Semaphore(maxPendingOffsets);
    this.inflightSemaphore = new AdjustableSemaphore(inflightQueueSize);
    metrics
        .subscriptions()
        .registerPendingOffsetsGauge(
            subscriptionName,
            maxPendingOffsetsSemaphore,
            slots -> (maxPendingOffsets - (double) slots.availablePermits()) / maxPendingOffsets);
  }

  public void setInflightSize(int inflightQueueSize) {
    this.inflightSemaphore.setMaxPermits(inflightQueueSize);
  }

  public void markAsProcessed(SubscriptionPartitionOffset subscriptionPartitionOffset) {
    inflightSemaphore.release();
    slots.put(subscriptionPartitionOffset, MessageState.PROCESSED);
  }

  public boolean tryAcquireSlot(Duration processingInterval) throws InterruptedException {
    if (inflightSemaphore.tryAcquire(processingInterval.toMillis(), TimeUnit.MILLISECONDS)) {
      if (maxPendingOffsetsSemaphore.tryAcquire(
          processingInterval.toMillis(), TimeUnit.MILLISECONDS)) {
        return true;
      }
      inflightSemaphore.release();
    }
    return false;
  }

  public void markAsInflight(SubscriptionPartitionOffset subscriptionPartitionOffset) {
    slots.put(subscriptionPartitionOffset, MessageState.INFLIGHT);
  }

  public Map<SubscriptionPartitionOffset, MessageState>
      getOffsetsSnapshotAndReleaseProcessedSlots() {
    int permitsReleased = 0;
    Map<SubscriptionPartitionOffset, MessageState> offsetSnapshot = new HashMap<>();

    for (Map.Entry<SubscriptionPartitionOffset, MessageState> entry : slots.entrySet()) {
      offsetSnapshot.put(entry.getKey(), entry.getValue());
      if (entry.getValue() == MessageState.PROCESSED) {
        slots.remove(entry.getKey());
        permitsReleased++;
      }
    }
    maxPendingOffsetsSemaphore.release(permitsReleased);
    return offsetSnapshot;
  }

  public void releaseSlot() {
    inflightSemaphore.release();
    maxPendingOffsetsSemaphore.release();
  }
}
