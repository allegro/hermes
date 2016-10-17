package pl.allegro.tech.hermes.consumers.consumer.offset;

import com.codahale.metrics.Timer;
import com.google.common.collect.Sets;
import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
 * This committer class holds internal state in form of inflightOffsets and failedToCommitOffsets set.
 * inlfightOffsets are all offsets that are currently in inflight state.
 * failedToCommitOffsets are offsets that could not be committed in previous algorithm iteration
 * <p>
 * In scheduled periods, commit algorithm is run. It has three phases. First one is draining the queues and performing
 * reductions:
 * * drain committedOffsets queue to collection - it needs to be done before draining inflights, so this collection
 * will not grow anymore, resulting in having inflights unmatched by commits; commits are incremented by 1 to match
 * Kafka commit definition
 * * add all previously uncommitted offsets from failedToCommitOffsets collection to committedOffsets and clear
 * failedToCommitOffsets collection
 * * drain inflightOffset
 * <p>
 * Second phase is calculating the offsets:
 * <p>
 * * calculate maximal committed offset for each subscription & partition
 * * calculate minimal inflight offset for each subscription & partition
 * <p>
 * Third phase is choosing which offset to commit for each subscription/partition. This is the minimal value of
 * * maximum committed offset
 * * minimum inflight offset
 * <p>
 * This algorithm is very simple, memory efficient, can be performed in single thread and introduces no locks.
 */
public class OffsetCommitter implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(OffsetCommitter.class);

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    private final int offsetCommitPeriodSeconds;

    private final OffsetQueue offsetQueue;

    private final MessageCommitter messageCommitter;

    private final HermesMetrics metrics;

    private final Set<SubscriptionPartitionOffset> inflightOffsets = new HashSet<>();

    private final MpscArrayQueue<SubscriptionName> subscriptionsToCleanup = new MpscArrayQueue<>(1000);

    public OffsetCommitter(
            OffsetQueue offsetQueue,
            MessageCommitter messageCommitter,
            int offsetCommitPeriodSeconds,
            HermesMetrics metrics
    ) {
        this.offsetQueue = offsetQueue;
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
            Map<SubscriptionPartition, Long> maxCommittedOffsets = committedOffsetsReducer.reduced;

            ReducingConsumer inflightOffsetReducer = processInflightOffsets(committedOffsetsReducer.all);
            Map<SubscriptionPartition, Long> minInflightOffsets = inflightOffsetReducer.reduced;

            int scheduledToCommit = 0;
            OffsetsToCommit offsetsToCommit = new OffsetsToCommit();
            for (SubscriptionPartition partition : Sets.union(minInflightOffsets.keySet(), maxCommittedOffsets.keySet())) {
                long offset = Math.min(
                        minInflightOffsets.getOrDefault(partition, Long.MAX_VALUE),
                        maxCommittedOffsets.getOrDefault(partition, Long.MAX_VALUE)
                );
                if (offset >= 0 && offset < Long.MAX_VALUE) {
                    scheduledToCommit++;
                    offsetsToCommit.add(new SubscriptionPartitionOffset(partition, offset));
                }
            }

            messageCommitter.commitOffsets(offsetsToCommit);

            metrics.counter("offset-committer.committed").inc(scheduledToCommit);

            cleanupUnusedSubscriptions();
        } catch (Exception exception) {
            logger.error("Failed to run offset committer: {}", exception.getMessage(), exception);
        }
    }

    private ReducingConsumer processCommittedOffsets() {
        ReducingConsumer committedOffsetsReducer = new ReducingConsumer(Math::max, c -> c + 1);
        offsetQueue.drainCommittedOffsets(committedOffsetsReducer);
        committedOffsetsReducer.resetModifierFunction();
        return committedOffsetsReducer;
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

    public void removeUncommittedOffsets(SubscriptionName subscriptionName) {
        subscriptionsToCleanup.offer(subscriptionName);
    }

    private void cleanupUnusedSubscriptions() {
        Set<SubscriptionName> subscriptionNames = new HashSet<>();
        subscriptionsToCleanup.drain(subscriptionNames::add);
        for (Iterator<SubscriptionPartitionOffset> iterator = inflightOffsets.iterator(); iterator.hasNext(); ) {
            if (subscriptionNames.contains(iterator.next().getSubscriptionName())) {
                iterator.remove();
            }
        }
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
