package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

public interface MessageCommitter {

    void commitOffset(SubscriptionPartitionOffset subscriptionPartitionOffset) throws Exception;

    void removeOffset(TopicName topicName, String subscriptionName, KafkaTopicName topic, int partition) throws Exception;

}
