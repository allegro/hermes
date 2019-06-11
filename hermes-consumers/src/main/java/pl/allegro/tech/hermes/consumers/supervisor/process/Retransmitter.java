package pl.allegro.tech.hermes.consumers.supervisor.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.exception.RetransmissionException;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartition;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;

public class Retransmitter {

    private static final Logger logger = LoggerFactory.getLogger(Retransmitter.class);
    private static final long NON_EXISTENT_PARTITION_ASSIGNMENT_TERM = -1; // real one not needed for retransmission purposes

    private SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator;
    private String brokersClusterName;

    @Inject
    public Retransmitter(SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator,
                         ConfigFactory configs) {
        this.subscriptionOffsetChangeIndicator = subscriptionOffsetChangeIndicator;
        this.brokersClusterName = configs.getStringProperty(KAFKA_CLUSTER_NAME);
    }

    public void reloadOffsets(SubscriptionName subscriptionName, Consumer consumer) {
        logger.info("Reloading offsets for {}", subscriptionName);
        try {
            PartitionOffsets offsets = subscriptionOffsetChangeIndicator.getSubscriptionOffsets(
                    subscriptionName.getTopicName(), subscriptionName.getName(), brokersClusterName);

            for (PartitionOffset partitionOffset : offsets) {
                SubscriptionPartitionOffset subscriptionPartitionOffset = new SubscriptionPartitionOffset(
                        new SubscriptionPartition(partitionOffset.getTopic(), subscriptionName,
                                partitionOffset.getPartition(), NON_EXISTENT_PARTITION_ASSIGNMENT_TERM),
                        partitionOffset.getOffset());

                if (moveOffset(subscriptionName, consumer, subscriptionPartitionOffset)) {
                    subscriptionOffsetChangeIndicator.removeOffset(
                        subscriptionName.getTopicName(),
                        subscriptionName.getName(),
                        brokersClusterName,
                        partitionOffset.getTopic(),
                        partitionOffset.getPartition()
                    );
                    logger.info("Removed offset indicator for subscription={} and partition={}",
                            subscriptionName, subscriptionPartitionOffset.getPartition());
                }
            }
        } catch (Exception ex) {
            throw new RetransmissionException(ex);
        }
    }

    private boolean moveOffset(SubscriptionName subscriptionName,
                            Consumer consumer,
                            SubscriptionPartitionOffset subscriptionPartitionOffset) {
        try {
            return consumer.moveOffset(subscriptionPartitionOffset);
        } catch (IllegalStateException ex) {
            logger.warn("Cannot move offset for subscription={} and partition={} , possibly owned by different node",
                    subscriptionName, subscriptionPartitionOffset.getPartition(), ex);
            return false;
        }
    }
}
