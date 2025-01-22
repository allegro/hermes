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
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;

public class KafkaConsumerOffsetMover {

  private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerOffsetMover.class);

  private final SubscriptionName subscriptionName;
  private KafkaConsumer consumer;
  private ConsumerPartitionAssignmentState partitionAssignmentState;

  public KafkaConsumerOffsetMover(
      SubscriptionName subscriptionName,
      KafkaConsumer consumer,
      ConsumerPartitionAssignmentState partitionAssignmentState) {
    this.subscriptionName = subscriptionName;
    this.consumer = consumer;
    this.partitionAssignmentState = partitionAssignmentState;
  }

  public PartitionOffsets move(PartitionOffsets offsets) {
    PartitionOffsets movedOffsets = new PartitionOffsets();

    for (PartitionOffset offset : offsets) {
      if (move(offset)) {
        movedOffsets.add(offset);
      }
    }

    commit(movedOffsets);

    if (!movedOffsets.isEmpty()) {
      // Incrementing assignment term ensures that currently committed offsets won't be overwritten
      // by the events from the past which are concurrently processed by the consumer
      partitionAssignmentState.incrementTerm(subscriptionName);
    }

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
