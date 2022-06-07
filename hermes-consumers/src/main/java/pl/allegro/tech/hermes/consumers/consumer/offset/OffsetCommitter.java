package pl.allegro.tech.hermes.consumers.consumer.offset;

import com.codahale.metrics.Timer;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jctools.queues.MessagePassingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Note on algorithm used to calculate offsets to actually commit.
 * <p>
 * The idea behind this algorithm is that we would like to commit:
 * * maximal offset marked as committed
 * * but not larger than smallest inflight offset (smallest inflight - 1)
 * <p>
 * Important note! This class is Kafka OffsetCommiter, and so it perceives offsets in Kafka way. Most importantly
 * committed offset marks message that is read as first on Consumer restart (offset is inclusive for reading and
 * exclusive for writing).
 * <p>
 * There are two queues which are used by Consumers to report message state:
 * * inflightOffsets: message offsets that are currently being sent (inflight)
 * * committedOffsets: message offsets that are ready to get committed
 * <p>
 * This committer class holds internal state in form of inflightOffsets and maxCommittedOffsets collections.
 * * inlfightOffsets are all offsets that are currently in inflight state.
 * * maxCommittedOffsets are offsets (maximum per partition) of already committed messages that could not yet be committed
 * to kafka due to an existing inflight offset on the same partition
 *
 * <p>
 * In scheduled periods, commit algorithm is run. It has three phases. First one is draining the queues and performing
 * reductions:
 * * drain committedOffsets queue to collection - it needs to be done before draining inflights, so this collection
 * will not grow anymore, resulting in having inflights unmatched by commits; commits are incremented by 1 to match
 * Kafka commit definition
 * * update the maxCommittedOffsets map with largest committed offsets
 * * drain inflightOffsets
 * <p>
 * Second phase is calculating the offsets:
 * <p>
 * * calculate maximal committed offset for each subscription and partition
 * * calculate minimal inflight offset for each subscription and partition
 * <p>
 * Third phase is choosing which offset to commit for each subscription/partition. This is the minimal value of
 * * maximum committed offset
 * * minimum inflight offset
 * <p>
 * This algorithm is very simple, memory efficient, can be performed in single thread and introduces no locks.
 */
public class OffsetCommitter implements Runnable {


    private static final Logger logger = LoggerFactory.getLogger(OffsetCommitter.class);

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("offset-committer-%d").build());

    private final int offsetCommitPeriodSeconds;

    private final OffsetQueue offsetQueue;

    private final ConsumerPartitionAssignmentState partitionAssignmentState;
    private final MessageCommitter messageCommitter;

    private final HermesMetrics metrics;

    private final Set<SubscriptionPartitionOffset> inflightOffsets = new HashSet<>();
    private final Map<SubscriptionPartition, Long> maxCommittedOffsets = new HashMap<>();
    private final Map<SubscriptionName, AtomicLong> lastCommittedMessageTimestamps = new HashMap<>();

    public OffsetCommitter(
            OffsetQueue offsetQueue,
            ConsumerPartitionAssignmentState partitionAssignmentState,
            MessageCommitter messageCommitter,
            int offsetCommitPeriodSeconds,
            HermesMetrics metrics
    ) {
        this.offsetQueue = offsetQueue;
        this.partitionAssignmentState = partitionAssignmentState;
        this.messageCommitter = messageCommitter;
        this.offsetCommitPeriodSeconds = offsetCommitPeriodSeconds;
        this.metrics = metrics;
    }

    @Override
    public void run() {
        try (Timer.Context c = metrics.timer("offset-committer.duration").time()) {
            // committed offsets need to be drained first so that there is no possibility of new committed offsets
            // showing up after inflight queue is drained - this would lead to stall in committing offsets
            ReducingConsumer committedOffsetsReducer = processCommittedOffsets();

            // update stored max committed offsets with offsets drained from queue
            Map<SubscriptionPartition, Long> maxDrainedCommittedOffsets = committedOffsetsReducer.reduced;
            updateMaxCommittedOffsets(maxDrainedCommittedOffsets);

            ReducingConsumer inflightOffsetReducer = processInflightOffsets(committedOffsetsReducer.all);
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

            reportLastCommittedMessageTimestamp(offsetsToCommit);

            metrics.counter("offset-committer.obsolete").inc(obsoleteCount);
            metrics.counter("offset-committer.committed").inc(scheduledToCommitCount);

            cleanupStoredOffsetsWithObsoleteTerms();
        } catch (Exception exception) {
            logger.error("Failed to run offset committer: {}", exception.getMessage(), exception);
        }
    }

    private void reportLastCommittedMessageTimestamp(OffsetsToCommit offsetsToCommit) {
        offsetsToCommit.subscriptionNames().forEach(subscriptionName -> {
            try {
                SubscriptionPartitionOffset offsetToReport = offsetsToCommit.batchFor(subscriptionName).stream()
                        .min(Comparator.comparing(SubscriptionPartitionOffset::getLastCommittedMessageTimestamp))
                        .orElseThrow(NoSuchElementException::new);
                logger.info("reporting last committed timestamp: {}, for subscription: {}", offsetToReport.getLastCommittedMessageTimestamp(), offsetToReport.getSubscriptionName());
                long lag = System.currentTimeMillis() - offsetToReport.getLastCommittedMessageTimestamp();
                logger.info("lag for message is: {}", lag);
                updateGauge(subscriptionName);
            } catch (Exception exception) {
                logger.error("Failed to meter last committed message with error: {}", exception.getMessage(), exception);
            }
        });
    }

    private void updateGauge(SubscriptionName subscriptionName) {
        if (!lastCommittedMessageTimestamps.containsKey(subscriptionName)) {
            lastCommittedMessageTimestamps.put(subscriptionName, new AtomicLong(0L));
            metrics.registerGaugeForSubscription(
                    "last-committed-message-timestamp.",
                    subscriptionName,
                    () -> lastCommittedMessageTimestamps.get(subscriptionName).get()
            );
        }

        lastCommittedMessageTimestamps.get(subscriptionName).set(System.currentTimeMillis());
    }

    private ReducingConsumer processCommittedOffsets() {
        ReducingConsumer committedOffsetsReducer = new ReducingConsumer(Math::max, c -> c + 1);
        offsetQueue.drainCommittedOffsets(committedOffsetsReducer);
        committedOffsetsReducer.resetModifierFunction();
        return committedOffsetsReducer;
    }

    private void updateMaxCommittedOffsets(Map<SubscriptionPartition, Long> maxDrainedCommittedOffsets) {
        maxDrainedCommittedOffsets.forEach((partition, drainedOffset) ->
                maxCommittedOffsets.compute(partition, (p, storedOffset) ->
                        storedOffset == null || storedOffset < drainedOffset ? drainedOffset : storedOffset)
        );
    }

    private ReducingConsumer processInflightOffsets(Set<SubscriptionPartitionOffset> committedOffsets) {
        ReducingConsumer inflightOffsetReducer = new ReducingConsumer(Math::min);
        offsetQueue.drainInflightOffsets(o -> reduceIfNotCommitted(o, inflightOffsetReducer, committedOffsets));
        inflightOffsets.forEach(o -> reduceIfNotCommitted(o, inflightOffsetReducer, committedOffsets));

        inflightOffsets.clear();
        inflightOffsets.addAll(inflightOffsetReducer.all);

        return inflightOffsetReducer;
    }

    private void reduceIfNotCommitted(SubscriptionPartitionOffset offset,
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

        @Override
        public void accept(SubscriptionPartitionOffset p) {
            all.add(p);
            reduced.compute(
                    p.getSubscriptionPartition(),
                    (k, v) -> {
                        long offset = modifier.apply(p.getOffset());
                        return v == null ? offset : reductor.apply(v, offset);
                    }
            );
        }
    }
}
