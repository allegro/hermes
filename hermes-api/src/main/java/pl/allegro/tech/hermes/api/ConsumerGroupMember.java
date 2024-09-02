package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Set;

public class ConsumerGroupMember {
  private final String consumerId;
  private final String clientId;
  private final String host;

  private final Set<TopicPartition> partitions;

  @JsonCreator
  public ConsumerGroupMember(
      @JsonProperty("consumerId") String consumerId,
      @JsonProperty("clientId") String clientId,
      @JsonProperty("host") String host,
      @JsonProperty("partitions") Set<TopicPartition> partitions) {
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

  public Set<TopicPartition> getPartitions() {
    return partitions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsumerGroupMember that = (ConsumerGroupMember) o;
    return Objects.equals(consumerId, that.consumerId)
        && Objects.equals(clientId, that.clientId)
        && Objects.equals(host, that.host)
        && Objects.equals(partitions, that.partitions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(consumerId, clientId, host, partitions);
  }
}
