package pl.allegro.tech.hermes.consumers.consumer.offset;

import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import pl.allegro.tech.hermes.api.SubscriptionName;

public class OffsetCommitterConsumerRebalanceListener implements ConsumerRebalanceListener {

  private final SubscriptionName name;
  private final ConsumerPartitionAssignmentState state;

  public OffsetCommitterConsumerRebalanceListener(
      SubscriptionName name, ConsumerPartitionAssignmentState state) {
    this.name = name;
    this.state = state;
  }

  @Override
  public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
    state.revoke(name, integerPartitions(partitions));
  }

  @Override
  public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
    state.assign(name, integerPartitions(partitions));
  }

  private Set<Integer> integerPartitions(Collection<TopicPartition> partitions) {
    return partitions.stream().map(TopicPartition::partition).collect(toSet());
  }
}
