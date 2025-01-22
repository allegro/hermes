package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import java.util.Collections;
import org.apache.kafka.common.TopicPartition;
import pl.allegro.tech.hermes.common.kafka.KafkaConsumerPool;

public class LogEndOffsetChecker {

  private final KafkaConsumerPool kafkaConsumerPool;

  public LogEndOffsetChecker(KafkaConsumerPool kafkaConsumerPool) {
    this.kafkaConsumerPool = kafkaConsumerPool;
  }

  public long check(TopicPartition topicPartition) {
    return kafkaConsumerPool
        .get(topicPartition.topic(), topicPartition.partition())
        .endOffsets(Collections.singletonList(topicPartition))
        .get(topicPartition);
  }
}
