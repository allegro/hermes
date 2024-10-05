package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

public class KafkaConsumerOffsetMover {

  private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerOffsetMover.class);

  private final SubscriptionName subscriptionName;
  private KafkaConsumer consumer;

  public KafkaConsumerOffsetMover(SubscriptionName subscriptionName, KafkaConsumer consumer) {
    this.subscriptionName = subscriptionName;
    this.consumer = consumer;
  }

  public boolean move(PartitionOffset offset) {
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
}
