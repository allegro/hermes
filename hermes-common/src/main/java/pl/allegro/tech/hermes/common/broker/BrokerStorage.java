package pl.allegro.tech.hermes.common.broker;

import org.apache.kafka.common.TopicPartition;

import java.util.List;

public interface BrokerStorage {

    int readLeaderForPartition(TopicPartition topicAndPartition);

    List<Integer> readPartitionsIds(String topicName);
}
