package pl.allegro.tech.hermes.common.kafka;

import java.util.Objects;
import java.util.Set;

public class KafkaConsumerGroupMember {
    private final String consumerId;
    private final String clientId;
    private final String host;

    private final Set<KafkaTopicPartition> partitions;

    public KafkaConsumerGroupMember(String consumerId, String clientId, String host, Set<KafkaTopicPartition> partitions) {
        this.consumerId = consumerId;
        this.clientId = clientId;
        this.host = host;
        this.partitions = partitions;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getHost() {
        return host;
    }

    public Set<KafkaTopicPartition> getPartitions() {
        return partitions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaConsumerGroupMember that = (KafkaConsumerGroupMember) o;
        return Objects.equals(consumerId, that.consumerId) &&
                Objects.equals(clientId, that.clientId) &&
                Objects.equals(host, that.host) &&
                Objects.equals(partitions, that.partitions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumerId, clientId, host, partitions);
    }
}
