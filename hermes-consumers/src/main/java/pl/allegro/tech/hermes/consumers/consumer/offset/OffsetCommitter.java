package pl.allegro.tech.hermes.consumers.consumer.offset;

import org.jctools.queues.MpscArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * Note on algorithm used to calculate offsets to actually commit.
 * <p>
 * There are two queues which are used by Consumers to report message state:
 * * inflightOffsets: message offsets that are currently being sent (inflight)
 * * committedOffsets: message offsets that are ready to get committed
 * <p>
 * This committer class holds internal state in form of inflightOffsets and failedToCommitOffsets set.
 * inlfightOffsets are all offsets that are currently in inflight state.
 * failedToCommitOffsets are offsets that could not be committed in previous algorithm iteration
 * <p>
 * In scheduled periods, commit algorithm is run:
 * * drain inflightOffsets queue to inlfightOffsets set
 * * add all failedToCommitOffsets to inlightOffsets set and clear the failed set
 * * calculate max offset for each topic & partition from inflightOffsets set
 * * drain committedOffsets by removing all elements from inflightOffsets set
 * * calculate min offset for each topic & partition from inflightOffsets set
 * <p>
 * For each item in topic & partition max offsets do:
 * * if there is min offset for topic & partition -> commit this offset - 1
 * * if there is no min offset, commit max (all other offsets were committed)
 * <p>
 * This algorithm is very simple, memory efficient, can be performed in single thread and introduces no locks.
 */
public class OffsetCommitter implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(OffsetCommitter.class);

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    private final int offsetCommitPeriodSeconds;

    private final OffsetQueue offsetQueue;

    private final List<MessageCommitter> messageCommitters;

    private final HermesMetrics metrics;

    private final Set<SubscriptionPartitionOffset> inflightOffsets = new HashSet<>();

    private final Set<SubscriptionPartitionOffset> failedToCommitOffsets = new HashSet<>();

    private final MpscArrayQueue<SubscriptionName> subscriptionsToCleanup = new MpscArrayQueue<>(1000);

    public OffsetCommitter(
            OffsetQueue offsetQueue,
            List<MessageCommitter> messageCommitters,
            int offsetCommitPeriodSeconds,
            HermesMetrics metrics
    ) {
        this.offsetQueue = offsetQueue;
        this.messageCommitters = messageCommitters;
        this.offsetCommitPeriodSeconds = offsetCommitPeriodSeconds;
        this.metrics = metrics;
    }

    @Override
    public void run() {
        try {
            offsetQueue.drainInflightOffsets(inflightOffsets::add);
            inflightOffsets.addAll(failedToCommitOffsets);
            failedToCommitOffsets.clear();

            Map<SubscriptionPartition, Long> maxInflightOffsets = calculateInflightOffsets(Math::max);

            offsetQueue.drainCommittedOffsets(inflightOffsets::remove);

            Map<SubscriptionPartition, Long> minInflightOffsets = calculateInflightOffsets(Math::min);

            Set<SubscriptionPartitionOffset> offsetsToCommit = new HashSet<>();
            maxInflightOffsets.forEach((k, v) -> {
                if (minInflightOffsets.containsKey(k)) {
                    offsetsToCommit.add(new SubscriptionPartitionOffset(k, minInflightOffsets.get(k) - 1));
                } else {
                    offsetsToCommit.add(new SubscriptionPartitionOffset(k, v));
                }
            });

            for (SubscriptionPartitionOffset offset : offsetsToCommit) {
                commit(offset);
            }
            metrics.counter("offset-committer.committed").inc(offsetsToCommit.size());
            metrics.counter("offset-committer.failed").inc(failedToCommitOffsets.size());

            cleanupUnusedSubscriptions();
        } catch(Exception exception) {
            logger.error("Failed to run offset committer: {}", exception.getMessage(), exception);
        }
    }

    private Map<SubscriptionPartition, Long> calculateInflightOffsets(BiFunction<Long, Long, Long> comparer) {
        Map<SubscriptionPartition, Long> calculatedOnflightOffsets = new HashMap<>();
        inflightOffsets.forEach(p -> calculatedOnflightOffsets.compute(
                p.getSubscriptionPartition(),
                (k, v) -> v == null ? p.getOffset() : comparer.apply(v, p.getOffset())
        ));
        return calculatedOnflightOffsets;
    }

    private void commit(SubscriptionPartitionOffset offset) {
        for (MessageCommitter committer : messageCommitters) {
            try {
                committer.commitOffset(offset);
            } catch (Exception e) {
                // it will be treated as min in next iteration, so it needs the + 1
                failedToCommitOffsets.add(new SubscriptionPartitionOffset(offset.getSubscriptionPartition(), offset.getOffset() + 1));
                logger.error("Failed to commit offset {} using {} committer", offset, committer.getClass().getSimpleName(), e);
            }
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
}
