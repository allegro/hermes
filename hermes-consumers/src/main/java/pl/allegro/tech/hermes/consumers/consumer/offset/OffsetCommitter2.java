package pl.allegro.tech.hermes.consumers.consumer.offset;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jctools.queues.MessagePassingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.queue.MpscQueue;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;

/**
 * <p>Note on algorithm used to calculate offsets to actually commit.
 * The idea behind this algorithm is that we would like to commit:</p>
 *
 * <ul>
 * <li>maximal offset marked as committed,</li>
 * <li>but not larger than smallest inflight offset (smallest inflight - 1).</li>
 * </ul>
 *
 * <p>Important note! This class is Kafka <code>OffsetCommiter</code>, and so it perceives offsets in Kafka way. Most importantly
 * committed offset marks message that is read as first on Consumer restart (offset is inclusive for reading and
 * exclusive for writing).</p>
 *
 * <p>There are two queues which are used by Consumers to report message state:</p>
 * <ul>
 * <li><code>inflightOffsets</code>: message offsets that are currently being sent (inflight),</li>
 * <li><code>commitedOffsets</code>: message offsets that are ready to get committed.</li>
 * </ul>
 *
 * <p>This committer class holds internal state in form of inflightOffsets and maxCommittedOffsets collections.</p>
 * <ul>
 * <li><code>inflightOffsets</code> are all offsets that are currently in inflight state,</li>
 * <li><code>maxCommittedOffsets</code> are offsets (maximum per partition) of already committed messages that could not yet be committed
 * to kafka due to an existing inflight offset on the same partition.</li>
 * </ul>
 *
 * <p>In scheduled periods, commit algorithm is run. It has three phases. First one is draining the queues and performing
 * reductions:</p>
 * <ul>
 * <li>drain <code>committedOffsets</code> queue to collection - it needs to be done before draining inflights, so this collection
 * will not grow anymore, resulting in having inflights unmatched by commits; commits are incremented by 1 to match
 * Kafka commit definition,</li>
 * <li>update the <code>maxCommittedOffsets</code> map with largest committed offsets,</li>
 * <li>drain <code>inflightOffsets</code>.</li>
 * </ul>
 *
 * <p>Second phase is calculating the offsets:</p>
 * <ul>
 * <li>calculate maximal committed offset for each subscription and partition,</li>
 * <li>calculate minimal inflight offset for each subscription and partition.</li>
 * </ul>
 *
 * <p>Third phase is choosing which offset to commit for each subscription/partition. This is the minimal value of:</p>
 * <ul>
 * <li>maximum committed offset,</li>
 * <li>minimum inflight offset.</li>
 * </ul>
 * <p>This algorithm is very simple, memory efficient, can be performed in single thread and introduces no locks.</p>
 */
public class OffsetCommitter2 implements Runnable {


    private static final Logger logger = LoggerFactory.getLogger(OffsetCommitter2.class);

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("offset-committer-%d").build());

    private final int offsetCommitPeriodSeconds;

    private final ConsumerPartitionAssignmentState partitionAssignmentState;
    private final MessageCommitter messageCommitter;

    private final HermesCounter obsoleteCounter;
    private final HermesCounter committedCounter;
    private final HermesTimer timer;

    private final Set<SubscriptionPartitionOffset> inflightOffsets = new HashSet<>();
    private final Map<SubscriptionPartition, Long> maxCommittedOffsets = new HashMap<>();

    public OffsetCommitter2(
            OffsetQueue offsetQueue,
            ConsumerPartitionAssignmentState partitionAssignmentState,
            MessageCommitter messageCommitter,
            int offsetCommitPeriodSeconds,
            MetricsFacade metrics
    ) {
        this.partitionAssignmentState = partitionAssignmentState;
        this.messageCommitter = messageCommitter;
        this.offsetCommitPeriodSeconds = offsetCommitPeriodSeconds;
        this.obsoleteCounter = metrics.offsetCommits().obsoleteCounter();
        this.committedCounter = metrics.offsetCommits().committedCounter();
        this.timer = metrics.offsetCommits().duration();
    }

    public void calculateOffsetsToBeCommitted(MpscQueue<SubscriptionPartitionOffset> inflightOffsetsQueue,
                                              MpscQueue<SubscriptionPartitionOffset> deliveredOffsetsQueue) {
        try (HermesTimerContext ignored = timer.time()) {
            // committed offsets need to be drained first so that there is no possibility of new committed offsets
            // showing up after inflight queue is drained - this would lead to stall in committing offsets
            ReducingConsumer committedOffsetsReducer = processCommittedOffsets(deliveredOffsetsQueue);

            // update stored max committed offsets with offsets drained from queue
            Map<SubscriptionPartition, Long> maxDrainedCommittedOffsets = committedOffsetsReducer.reduced;
            updateMaxCommittedOffsets(maxDrainedCommittedOffsets);

            ReducingConsumer inflightOffsetReducer = processInflightOffsets(committedOffsetsReducer.all, inflightOffsetsQueue);
            Map<SubscriptionPartition, Long> minInflightOffsets = inflightOffsetReducer.reduced;

            int scheduledToCommitCount = 0;
            int obsoleteCount = 0;

            Set<SubscriptionPartition> committedOffsetToBeRemoved = new HashSet<>();

            OffsetsToCommit offsetsToCommit = new OffsetsToCommit();
            for (SubscriptionPartition partition : Sets.union(minInflightOffsets.keySet(), maxCommittedOffsets.keySet())) {
                if (partitionAssignmentState.isAssignedPartitionAtCurrentTerm(partition)) {
                    long minInflight = minInflightOffsets.getOrDefault(partition, Long.MAX_VALUE);
                    long maxCommitted = maxCommittedOffsets.getOrDefault(partition, Long.MAX_VALUE);

                    long offsetToBeCommitted = Math.min(minInflight, maxCommitted);
                    if (offsetToBeCommitted >= 0 && offsetToBeCommitted < Long.MAX_VALUE) {
                        scheduledToCommitCount++;
                        offsetsToCommit.add(new SubscriptionPartitionOffset(partition, offsetToBeCommitted));

                        // if we just committed the maximum possible offset for partition, we can safely forget about it
                        if (maxCommitted == offsetToBeCommitted) {
                            committedOffsetToBeRemoved.add(partition);
                        }
                    } else {
                        logger.warn("Skipping offset out of bounds for subscription {}: partition={}, offset={}",
                                partition.getSubscriptionName(), partition.getPartition(), offsetToBeCommitted);
                    }
                } else {
                    obsoleteCount++;
                }
            }
            committedOffsetToBeRemoved.forEach(maxCommittedOffsets::remove);
            messageCommitter.commitOffsets(offsetsToCommit);

            obsoleteCounter.increment(obsoleteCount);
            committedCounter.increment(scheduledToCommitCount);

            cleanupStoredOffsetsWithObsoleteTerms();
        } catch (Exception exception) {
            logger.error("Failed to run offset committer: {}", exception.getMessage(), exception);
        }
    }

    // | 1 | 2 | 3 | 7 | 10 | 11
    // | 3 | 7 | 10 | 11

    private ReducingConsumer processCommittedOffsets(MpscQueue<SubscriptionPartitionOffset> deliveredOffsetsQueue) {
        ReducingConsumer committedOffsetsReducer = new ReducingConsumer(Math::max, c -> c + 1);
        deliveredOffsetsQueue.drain(committedOffsetsReducer);
        committedOffsetsReducer.resetModifierFunction();
        return committedOffsetsReducer;
    }

    private void updateMaxCommittedOffsets(Map<SubscriptionPartition, Long> maxDrainedCommittedOffsets) {
        maxDrainedCommittedOffsets.forEach((partition, drainedOffset) ->
                maxCommittedOffsets.compute(partition, (p, storedOffset) ->
                        storedOffset == null || storedOffset < drainedOffset ? drainedOffset : storedOffset)
        );
    }

    private ReducingConsumer processInflightOffsets(Set<SubscriptionPartitionOffset> deliveredOffsets,
                                                    MpscQueue<SubscriptionPartitionOffset> inflightOffsetsQueue) {
        ReducingConsumer inflightOffsetReducer = new ReducingConsumer(Math::min);
        // najmniejsza niewysłana wiadomość

        // process inflights z obecnej iteracji
        inflightOffsetsQueue.drain(o -> reduceIfNotDelivered(o, inflightOffsetReducer, deliveredOffsets));
        // process inflights z poprzedniej iteracji
        inflightOffsets.forEach(o -> reduceIfNotDelivered(o, inflightOffsetReducer, deliveredOffsets));

        inflightOffsets.clear();
        inflightOffsets.addAll(inflightOffsetReducer.all);

        return inflightOffsetReducer;
    }

    private void reduceIfNotDelivered(SubscriptionPartitionOffset offset,
                                      ReducingConsumer inflightOffsetReducer,
                                      Set<SubscriptionPartitionOffset> committedOffsets) {
        if (!committedOffsets.contains(offset)) {
            inflightOffsetReducer.accept(offset);
        }
    }

    private void cleanupStoredOffsetsWithObsoleteTerms() {
        inflightOffsets.removeIf(o -> !partitionAssignmentState.isAssignedPartitionAtCurrentTerm(o.getSubscriptionPartition()));
        maxCommittedOffsets.entrySet().removeIf(entry -> !partitionAssignmentState.isAssignedPartitionAtCurrentTerm(entry.getKey()));
    }

    public void start() {
        scheduledExecutor.scheduleWithFixedDelay(this,
                offsetCommitPeriodSeconds,
                offsetCommitPeriodSeconds,
                TimeUnit.SECONDS
        );
    }

    public void shutdown() {
        scheduledExecutor.shutdown();
    }

    private static final class ReducingConsumer implements MessagePassingQueue.Consumer<SubscriptionPartitionOffset> {
        private final BiFunction<Long, Long, Long> reductor;
        private Function<Long, Long> modifier;
        private final Map<SubscriptionPartition, Long> reduced = new HashMap<>();
        private final Set<SubscriptionPartitionOffset> all = new HashSet<>();

        private ReducingConsumer(BiFunction<Long, Long, Long> reductor, Function<Long, Long> offsetModifier) {
            this.reductor = reductor;
            this.modifier = offsetModifier;
        }

        private ReducingConsumer(BiFunction<Long, Long, Long> reductor) {
            this(reductor, Function.identity());
        }

        private void resetModifierFunction() {
            this.modifier = Function.identity();
        }

        // new ReducingConsumer(Math::max, c -> c + 1);
        // | 1 | 2 | 10 | 11 | 12 | 13
        @Override
        public void accept(SubscriptionPartitionOffset p) {
            // p = 1
            // p = 2
            // p = 10
            all.add(p);
            reduced.compute(
                    p.getSubscriptionPartition(),
                    (k, v) -> {
                        // offset = 2

                        // v = 2
                        // modifier =  c -> c + 1
                        long offset = modifier.apply(p.getOffset());
                        // offset = 3
                        // offset = 11
                        // reductor = Math::max
                        return v == null ? offset : reductor.apply(v, offset);
                        // reduced = 11
                    }
            );
        }
    }
}
