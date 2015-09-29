package pl.allegro.tech.hermes.consumers.consumer.offset;

import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;

import java.util.Objects;

class TopicPartition {

    private final KafkaTopicName topic;
    private final int partition;

    TopicPartition(KafkaTopicName topic, int partition) {
        this.topic = topic;
        this.partition = partition;
    }

    public KafkaTopicName getTopic() {
        return topic;
    }

    public int getPartition() {
        return partition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TopicPartition that = (TopicPartition) o;
        return Objects.equals(partition, that.partition)
                && Objects.equals(topic, that.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, partition);
    }

}
