package pl.allegro.tech.hermes.consumers.consumer.receiver;

import java.util.Optional;
import java.util.Set;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
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
  public boolean moveOffset(PartitionOffset offset) {
    throw new ConsumerNotInitializedException();
  }
}
