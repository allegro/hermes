package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

public class SetSubscriptionOffsetData {
    private final TopicName topicName;
    private final String subscriptionName;
    private final String brokersClusterName;
    private final PartitionOffset partitionOffset;

    public SetSubscriptionOffsetData(TopicName topicName, String subscriptionName, String brokersClusterName,
                 PartitionOffset partitionOffset) {
        this.topicName = topicName;
        this.subscriptionName = subscriptionName;
        this.brokersClusterName = brokersClusterName;
        this.partitionOffset = partitionOffset;
    }

    public TopicName getTopicName() {
        return topicName;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public String getBrokersClusterName() {
        return brokersClusterName;
    }

    public PartitionOffset getPartitionOffset() {
        return partitionOffset;
    }
}
