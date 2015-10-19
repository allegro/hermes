package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

public interface MessageCommitter {

    void commitOffset(Subscription subscription, PartitionOffset partitionOffset) throws Exception;

    void removeOffset(TopicName topicName, String subscriptionName, KafkaTopicName topic, int partition) throws Exception;

}
