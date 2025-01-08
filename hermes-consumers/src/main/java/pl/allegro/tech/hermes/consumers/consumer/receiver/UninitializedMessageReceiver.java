package pl.allegro.tech.hermes.consumers.consumer.receiver;

import java.util.Optional;
import java.util.Set;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

public class UninitializedMessageReceiver implements MessageReceiver {
  @Override
  public Optional<Message> next() {
    throw new ConsumerNotInitializedException();
  }

  @Override
  public void commit(Set<SubscriptionPartitionOffset> offsets) {
    throw new ConsumerNotInitializedException();
  }

  @Override
  public PartitionOffsets moveOffset(PartitionOffsets offsets) {
    throw new ConsumerNotInitializedException();
  }

  @Override
  public Set<Integer> getAssignedPartitions() {
    throw new ConsumerNotInitializedException();
  }
}
