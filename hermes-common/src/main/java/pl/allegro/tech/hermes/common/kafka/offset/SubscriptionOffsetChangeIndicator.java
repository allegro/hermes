package pl.allegro.tech.hermes.common.kafka.offset;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;

import java.util.List;

public interface SubscriptionOffsetChangeIndicator {

    void setSubscriptionOffset(TopicName topicName, String subscriptionName, String brokersClusterName, PartitionOffset partitionOffset);

    PartitionOffsets getSubscriptionOffsets(TopicName topic, String subscriptionName, String brokersClusterName);

    boolean areOffsetsMoved(TopicName topicName, String subscriptionName, String brokersClusterName,
                            KafkaTopic kafkaTopic, List<Integer> partitionIds);


    void removeOffset(TopicName topicName, String subscriptionName, String brokersClusterName,
                      KafkaTopicName kafkaTopicName, int partitionId);
}
