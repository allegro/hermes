package pl.allegro.tech.hermes.consumers.consumer.offset;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;

/**
 * Note on algorithm used to calculate offsets to actually commit. The idea behind this algorithm is
 * that we would like to commit:
 *
 * <ul>
 *   <li>maximal offset marked as committed,
 *   <li>but not larger than smallest inflight offset (smallest inflight - 1).
 * </ul>
 *
 * <p>Important note! This class is Kafka <code>OffsetCommiter</code>, and so it perceives offsets
 * in Kafka way. Most importantly committed offset marks message that is read as first on Consumer
 * restart (offset is inclusive for reading and exclusive for writing).
 *
 * <p>There are two queues which are used by Consumers to report message state:
 *
 * <ul>
 *   <li><code>inflightOffsets</code>: message offsets that are currently being sent (inflight),
 *   <li><code>commitedOffsets</code>: message offsets that are ready to get committed.
 * </ul>
 *
 * <p>This committer class holds internal state in form of inflightOffsets and maxCommittedOffsets
 * collections.
 *
 * <ul>
 *   <li><code>inflightOffsets</code> are all offsets that are currently in inflight state,
 *   <li><code>maxCommittedOffsets</code> are offsets (maximum per partition) of already committed
 *       messages that could not yet be committed to kafka due to an existing inflight offset on the
 *       same partition.
 * </ul>
 *
 * <p>In scheduled periods, commit algorithm is run. It has three phases. First one is draining the
 * queues and performing reductions:
 *
 * <ul>
 *   <li>drain <code>committedOffsets</code> queue to collection - it needs to be done before
 *       draining inflights, so this collection will not grow anymore, resulting in having inflights
 *       unmatched by commits; commits are incremented by 1 to match Kafka commit definition,
 *   <li>update the <code>maxCommittedOffsets</code> map with largest committed offsets,
 *   <li>drain <code>inflightOffsets</code>.
 * </ul>
 *
 * <p>Second phase is calculating the offsets:
 *
 * <ul>
 *   <li>calculate maximal committed offset for each subscription and partition,
 *   <li>calculate minimal inflight offset for each subscription and partition.
 * </ul>
 *
 * <p>Third phase is choosing which offset to commit for each subscription/partition. This is the
 * minimal value of:
 *
 * <ul>
 *   <li>maximum committed offset,
 *   <li>minimum inflight offset.
 * </ul>
 *
 * <p>This algorithm is very simple, memory efficient, can be performed in single thread and
 * introduces no locks.
 */
public class OffsetCommitter {

  private static final Logger logger = LoggerFactory.getLogger(OffsetCommitter.class);

  private final ConsumerPartitionAssignmentState partitionAssignmentState;

  private final HermesCounter obsoleteCounter;
  private final HermesCounter committedCounter;
  private final HermesTimer timer;

  private final Set<SubscriptionPartitionOffset> inflightOffsets = new HashSet<>();
  private final Map<SubscriptionPartition, Long> maxCommittedOffsets = new HashMap<>();

  public OffsetCommitter(
      ConsumerPartitionAssignmentState partitionAssignmentState, MetricsFacade metrics) {
    this.partitionAssignmentState = partitionAssignmentState;
    this.obsoleteCounter = metrics.offsetCommits().obsoleteCounter();
    this.committedCounter = metrics.offsetCommits().committedCounter();
    this.timer = metrics.offsetCommits().duration();
  }

  public Set<SubscriptionPartitionOffset> calculateOffsetsToBeCommitted(
      Map<SubscriptionPartitionOffset, MessageState> offsets) {
    try (HermesTimerContext ignored = timer.time()) {
      List<SubscriptionPartitionOffset> processedOffsets = new ArrayList<>();
      for (Map.Entry<SubscriptionPartitionOffset, MessageState> entry : offsets.entrySet()) {
        if (entry.getValue() == MessageState.PROCESSED) {
          processedOffsets.add(entry.getKey());
        }
      }

      List<SubscriptionPartitionOffset> allOffsets = new ArrayList<>();
      for (Map.Entry<SubscriptionPartitionOffset, MessageState> entry : offsets.entrySet()) {
        allOffsets.add(entry.getKey());
      }

      ReducingConsumer processedOffsetsReducer = prepareProcessedOffsets(processedOffsets);

      // update stored max committed offsets with offsets drained from queue
      Map<SubscriptionPartition, Long> maxDrainedProcessedOffsets = processedOffsetsReducer.reduced;
      updateMaxProcessedOffsets(maxDrainedProcessedOffsets);

      ReducingConsumer inflightOffsetReducer =
          prepareInflightOffsets(processedOffsetsReducer.all, allOffsets);
      Map<SubscriptionPartition, Long> minInflightOffsets = inflightOffsetReducer.reduced;

      int scheduledToCommitCount = 0;
      int obsoleteCount = 0;

      Set<SubscriptionPartition> processedOffsetToBeRemoved = new HashSet<>();

      Set<SubscriptionPartitionOffset> offsetsToCommit = new HashSet<>();
      for (SubscriptionPartition partition :
          Sets.union(minInflightOffsets.keySet(), maxCommittedOffsets.keySet())) {
        if (partitionAssignmentState.isAssignedPartitionAtCurrentTerm(partition)) {
          long minInflight = minInflightOffsets.getOrDefault(partition, Long.MAX_VALUE);
          long maxCommitted = maxCommittedOffsets.getOrDefault(partition, Long.MAX_VALUE);

          long offsetToBeCommitted = Math.min(minInflight, maxCommitted);
          if (offsetToBeCommitted >= 0 && offsetToBeCommitted < Long.MAX_VALUE) {
            scheduledToCommitCount++;
            offsetsToCommit.add(new SubscriptionPartitionOffset(partition, offsetToBeCommitted));

            // if we just committed the maximum possible offset for partition, we can safely forget
            // about it
            if (maxCommitted == offsetToBeCommitted) {
              processedOffsetToBeRemoved.add(partition);
            }
          } else {
            logger.warn(
                "Skipping offset out of bounds for subscription {}: partition={}, offset={}",
                partition.getSubscriptionName(),
                partition.getPartition(),
                offsetToBeCommitted);
          }
        } else {
          obsoleteCount++;
        }
      }
      processedOffsetToBeRemoved.forEach(maxCommittedOffsets::remove);

      obsoleteCounter.increment(obsoleteCount);
      committedCounter.increment(scheduledToCommitCount);

      cleanupStoredOffsetsWithObsoleteTerms();

      return offsetsToCommit;
    } catch (Exception exception) {
      logger.error("Failed to run offset committer: {}", exception.getMessage(), exception);
    }
    return Set.of();
  }

  private ReducingConsumer prepareProcessedOffsets(
      List<SubscriptionPartitionOffset> processedOffsets) {
    ReducingConsumer processedOffsetsReducer = new ReducingConsumer(Math::max, c -> c + 1);
    drain(processedOffsets, processedOffsetsReducer);
    processedOffsetsReducer.resetModifierFunction();
    return processedOffsetsReducer;
  }

  private void updateMaxProcessedOffsets(
      Map<SubscriptionPartition, Long> maxDrainedCommittedOffsets) {
    maxDrainedCommittedOffsets.forEach(
        (partition, drainedOffset) ->
            maxCommittedOffsets.compute(
                partition,
                (p, storedOffset) ->
                    storedOffset == null || storedOffset < drainedOffset
                        ? drainedOffset
                        : storedOffset));
  }

  private ReducingConsumer prepareInflightOffsets(
      Set<SubscriptionPartitionOffset> processedOffsets,
      List<SubscriptionPartitionOffset> inflightOffsetsQueue) {
    // smallest undelivered message
    ReducingConsumer inflightOffsetReducer = new ReducingConsumer(Math::min);

    // process inflights from the current iteration
    drain(
        inflightOffsetsQueue,
        o -> reduceIfNotDelivered(o, inflightOffsetReducer, processedOffsets));

    // process inflights from the previous iteration
    inflightOffsets.forEach(o -> reduceIfNotDelivered(o, inflightOffsetReducer, processedOffsets));

    inflightOffsets.clear();
    inflightOffsets.addAll(inflightOffsetReducer.all);

    return inflightOffsetReducer;
  }

  private void reduceIfNotDelivered(
      SubscriptionPartitionOffset offset,
      ReducingConsumer inflightOffsetReducer,
      Set<SubscriptionPartitionOffset> committedOffsets) {
    if (!committedOffsets.contains(offset)) {
      inflightOffsetReducer.accept(offset);
    }
  }

  private void cleanupStoredOffsetsWithObsoleteTerms() {
    inflightOffsets.removeIf(
        o ->
            !partitionAssignmentState.isAssignedPartitionAtCurrentTerm(
                o.getSubscriptionPartition()));
    maxCommittedOffsets
        .entrySet()
        .removeIf(
            entry -> !partitionAssignmentState.isAssignedPartitionAtCurrentTerm(entry.getKey()));
  }

  private void drain(
      List<SubscriptionPartitionOffset> subscriptionPartitionOffsets,
      Consumer<SubscriptionPartitionOffset> consumer) {
    int size = subscriptionPartitionOffsets.size();
    for (int i = 0; i < size; i++) {
      SubscriptionPartitionOffset element = subscriptionPartitionOffsets.get(i);
      if (element != null) {
        consumer.accept(element);
      } else {
        logger.warn("Unexpected null value while draining queue [idx={}, size={}]", i, size);
        break;
      }
    }
  }

  private static final class ReducingConsumer implements Consumer<SubscriptionPartitionOffset> {
    private final BiFunction<Long, Long, Long> reductor;
    private Function<Long, Long> modifier;
    private final Map<SubscriptionPartition, Long> reduced = new HashMap<>();
    private final Set<SubscriptionPartitionOffset> all = new HashSet<>();

    private ReducingConsumer(
        BiFunction<Long, Long, Long> reductor, Function<Long, Long> offsetModifier) {
      this.reductor = reductor;
      this.modifier = offsetModifier;
    }

    private ReducingConsumer(BiFunction<Long, Long, Long> reductor) {
      this(reductor, Function.identity());
    }

    private void resetModifierFunction() {
      this.modifier = Function.identity();
    }

    @Override
    public void accept(SubscriptionPartitionOffset p) {
      all.add(p);
      reduced.compute(
          p.getSubscriptionPartition(),
          (k, v) -> {
            long offset = modifier.apply(p.getOffset());
            return v == null ? offset : reductor.apply(v, offset);
          });
    }
  }
}
