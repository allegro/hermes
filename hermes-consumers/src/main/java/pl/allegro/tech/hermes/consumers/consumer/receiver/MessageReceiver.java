package pl.allegro.tech.hermes.consumers.consumer.receiver;

import java.util.Optional;
import java.util.Set;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

public interface MessageReceiver {

  Optional<Message> next();

  default void stop() {}

  default void update(Subscription newSubscription) {}

  void commit(Set<SubscriptionPartitionOffset> offsets);

  boolean moveOffset(PartitionOffset offset);
}
