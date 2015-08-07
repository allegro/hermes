package pl.allegro.tech.hermes.common.broker;

import com.google.common.collect.Multimap;
import kafka.common.TopicAndPartition;

import java.util.List;
import java.util.Set;

public interface BrokerStorage {

    int readLeaderForPartition(TopicAndPartition topicAndPartition);

    Multimap<Integer, TopicAndPartition> readLeadersForPartitions(Set<TopicAndPartition> topicAndPartitionSet);

    BrokerDetails readBrokerDetails(Integer leaderId);

    List<Integer> readPartitionsIds(String topicName);
}
