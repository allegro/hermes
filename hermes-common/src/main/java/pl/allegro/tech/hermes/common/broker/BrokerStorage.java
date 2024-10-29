package pl.allegro.tech.hermes.common.broker;

import java.util.List;
import org.apache.kafka.common.TopicPartition;

public interface BrokerStorage {

  int readLeaderForPartition(TopicPartition topicAndPartition);

  List<Integer> readPartitionsIds(String topicName);
}
