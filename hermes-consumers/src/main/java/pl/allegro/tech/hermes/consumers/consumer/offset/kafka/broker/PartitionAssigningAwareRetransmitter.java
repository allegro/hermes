package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PartitionAssigningAwareRetransmitter implements ConsumerRebalanceListener {
    private static final Logger logger = LoggerFactory.getLogger(PartitionAssigningAwareRetransmitter.class);

    private final OffsetMover offsetMover;
    private final SubscriptionName subscriptionName;
    private final BlockingQueue<SubscriptionPartitionOffset> retransmissionQueue;

    public PartitionAssigningAwareRetransmitter(SubscriptionName subscriptionName, int queueSize, KafkaConsumer consumer) {
        this(subscriptionName, queueSize, new KafkaConsumerOffsetMover(consumer));
    }

    public PartitionAssigningAwareRetransmitter(SubscriptionName subscriptionName, int queueSize, OffsetMover offsetMover) {
        this.offsetMover = offsetMover;
        this.subscriptionName = subscriptionName;
        this.retransmissionQueue = new ArrayBlockingQueue<>(queueSize);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        // not interesting for retransmission
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        if (!retransmissionQueue.isEmpty()) {
            logger.info("Detected scheduled retransmission for subscription {}", subscriptionName);
            moveScheduledOffsets();
        }
    }

    public void moveOffsetOrSchedule(SubscriptionPartitionOffset offset) {
        try {
            logger.info("Moving offset for subscription {} {}", offset.getSubscriptionName(), offset.toString());
            offsetMover.move(offset);
        } catch (PartitionNotAssignedException ex) {
            logger.info("Failed to move offset right now, reason: {}", ex.getMessage());
            boolean scheduled = retransmissionQueue.offer(offset);
            if (scheduled) {
                logger.info("Scheduled retransmission for subscription {} on next rebalance," +
                        " offset {}", subscriptionName, offset.toString());
            } else {
                logger.info("Failed to schedule new retransmission for subscription {}," +
                        "there is already retransmission scheduled on next rebalance.", subscriptionName);
            }
        }
    }

    public boolean isQueueEmpty() {
        return retransmissionQueue.isEmpty();
    }

    private void moveScheduledOffsets() {
        List<SubscriptionPartitionOffset> offsets = new ArrayList<>();
        retransmissionQueue.drainTo(offsets);
        offsets.forEach(offset -> {
            try {
                offsetMover.move(offset);
            } catch (Exception ex) {
                logger.info("Still cannot move offset after rebalance for partition {} for subscription {}," +
                                " possibly owned by different node",
                        offset.getPartition(), offset.getSubscriptionName(), ex);
            }
        });
    }

    interface OffsetMover {
        void move(SubscriptionPartitionOffset offset) throws PartitionNotAssignedException;
    }
}
