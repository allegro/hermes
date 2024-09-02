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
              subscriptionName.getTopicName(), subscriptionName.getName(), brokersClusterName);

      for (PartitionOffset partitionOffset : offsets) {
        if (moveOffset(subscriptionName, consumer, partitionOffset)) {
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
      }
    } catch (Exception ex) {
      throw new RetransmissionException(ex);
    }
  }

  private boolean moveOffset(
      SubscriptionName subscriptionName, Consumer consumer, PartitionOffset partitionOffset) {
    try {
      return consumer.moveOffset(partitionOffset);
    } catch (IllegalStateException ex) {
      logger.warn(
          "Cannot move offset for subscription={} and partition={} , possibly owned by different node",
          subscriptionName,
          partitionOffset.getPartition(),
          ex);
      return false;
    }
  }
}
