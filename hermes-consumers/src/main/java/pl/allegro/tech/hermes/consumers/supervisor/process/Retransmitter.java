package pl.allegro.tech.hermes.consumers.supervisor.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.exception.RetransmissionException;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;

public class Retransmitter {

  private static final Logger logger = LoggerFactory.getLogger(Retransmitter.class);

  private final SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator;
  private final String brokersClusterName;

  public Retransmitter(
      SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator,
      String kafkaClusterName) {
    this.subscriptionOffsetChangeIndicator = subscriptionOffsetChangeIndicator;
    this.brokersClusterName = kafkaClusterName;
  }

  public void reloadOffsets(SubscriptionName subscriptionName, Consumer consumer) {
    logger.info("Reloading offsets for {}", subscriptionName);
    try {
      PartitionOffsets offsets =
          subscriptionOffsetChangeIndicator.getSubscriptionOffsets(
              subscriptionName.getTopicName(),
              subscriptionName.getName(),
              brokersClusterName,
              consumer.getAssignedPartitions());

      PartitionOffsets movedOffsets = consumer.moveOffset(offsets);

      for (PartitionOffset partitionOffset : movedOffsets) {
        subscriptionOffsetChangeIndicator.removeOffset(
            subscriptionName.getTopicName(),
            subscriptionName.getName(),
            brokersClusterName,
            partitionOffset.getTopic(),
            partitionOffset.getPartition());
        logger.info(
            "Removed offset indicator for subscription={} and partition={}",
            subscriptionName,
            partitionOffset.getPartition());
      }
    } catch (Exception ex) {
      throw new RetransmissionException(ex);
    }
  }
}
