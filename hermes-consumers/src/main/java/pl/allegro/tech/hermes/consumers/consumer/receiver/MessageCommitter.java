package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.consumers.consumer.offset.FailedToCommitOffsets;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsToCommit;

public interface MessageCommitter {

    FailedToCommitOffsets commitOffsets(OffsetsToCommit offsetsToCommit);

    void removeOffset(TopicName topicName, String subscriptionName, KafkaTopicName topic, int partition) throws Exception;

}
