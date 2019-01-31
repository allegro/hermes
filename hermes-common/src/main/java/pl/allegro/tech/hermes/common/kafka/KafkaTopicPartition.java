package pl.allegro.tech.hermes.common.kafka;

import java.util.Objects;

public class KafkaTopicPartition {
    private final int partition;
    private final String topic;

    public KafkaTopicPartition(int partition, String topic) {
        this.partition = partition;
        this.topic = topic;
    }

    public int getPartition() {
        return partition;
    }

    public String getTopic() {
        return topic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaTopicPartition that = (KafkaTopicPartition) o;
        return partition == that.partition &&
                Objects.equals(topic, that.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partition, topic);
    }
}
