package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets;

public class KafkaConsumerOffsetMover {

  private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerOffsetMover.class);

  private final SubscriptionName subscriptionName;
  private KafkaConsumer consumer;

  public KafkaConsumerOffsetMover(SubscriptionName subscriptionName, KafkaConsumer consumer) {
    this.subscriptionName = subscriptionName;
    this.consumer = consumer;
  }

  public PartitionOffsets move(PartitionOffsets offsets) {
    PartitionOffsets movedOffsets = new PartitionOffsets();

    for (PartitionOffset offset : offsets) {
      if (move(offset)) {
        movedOffsets.add(offset);
      }
    }

    commit(movedOffsets);

    return movedOffsets;
  }

  private boolean move(PartitionOffset offset) {
    try {
      TopicPartition tp = new TopicPartition(offset.getTopic().asString(), offset.getPartition());
      if (consumer.assignment().contains(tp)) {
        logger.info(
            "Moving offset for assigned partition {} on subscription {}",
            offset.getPartition(),
            subscriptionName);
        consumer.seek(tp, offset.getOffset());
        return true;
      } else {
        logger.info(
            "Not assigned to partition {} on subscription {}",
            offset.getPartition(),
            subscriptionName);
        return false;
      }
    } catch (IllegalStateException ex) {
      logger.error(
          "Failed to move offset for subscription={}, partition={}, offset={}",
          subscriptionName,
          offset.getPartition(),
          offset.getOffset(),
          ex);
      return false;
    }
  }

  private void commit(PartitionOffsets partitionOffsets) {
    try {
      Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new LinkedHashMap<>();
      for (PartitionOffset partitionOffset : partitionOffsets) {
        offsetsToCommit.put(
            new TopicPartition(
                partitionOffset.getTopic().asString(), partitionOffset.getPartition()),
            new OffsetAndMetadata(partitionOffset.getOffset()));
      }
      if (!offsetsToCommit.isEmpty()) {
        consumer.commitSync(offsetsToCommit);
      }
    } catch (Exception e) {
      logger.error(
          "Failed to commit offsets while trying to move them for subscription {}",
          subscriptionName,
          e);
    }
  }
}
