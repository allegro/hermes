package pl.allegro.tech.hermes.common.kafka.offset;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;

public interface SubscriptionOffsetChangeIndicator {

    void setSubscriptionOffset(TopicName topicName, String subscriptionName, String brokersClusterName, PartitionOffset partitionOffset);

    PartitionOffsets getSubscriptionOffsets(TopicName topic, String subscriptionName, String brokersClusterName);

}
