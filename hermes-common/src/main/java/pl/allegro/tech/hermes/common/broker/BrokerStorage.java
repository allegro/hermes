package pl.allegro.tech.hermes.common.broker;

import com.google.common.collect.Multimap;
import kafka.common.TopicAndPartition;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.List;
import java.util.Set;

public interface BrokerStorage {

    void setSubscriptionOffset(TopicName topicName, String subscriptionName, int partitionId, Long offset);

    int readLeaderForPartition(TopicAndPartition topicAndPartition);

    Multimap<Integer, TopicAndPartition> readLeadersForPartitions(Set<TopicAndPartition> topicAndPartitionSet);

    BrokerDetails readBrokerDetails(Integer leaderId);

    List<Integer> readPartitionsIds(String topicName);
}
