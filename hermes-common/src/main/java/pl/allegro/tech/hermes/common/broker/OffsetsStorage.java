package pl.allegro.tech.hermes.common.broker;

import pl.allegro.tech.hermes.api.TopicName;

public interface OffsetsStorage {

    void setSubscriptionOffset(TopicName topicName, String subscriptionName, int partitionId, long offset);

    long getSubscriptionOffset(TopicName topicName, String subscriptionName, int partitionId);
}
