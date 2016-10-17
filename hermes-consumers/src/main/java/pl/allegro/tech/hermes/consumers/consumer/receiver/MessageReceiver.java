package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

import java.util.Optional;
import java.util.Set;

public interface MessageReceiver {

    Optional<Message> next();

    default void stop() {}

    default void update(Subscription newSubscription) {}

    void commit(Set<SubscriptionPartitionOffset> offsets);

    void moveOffset(SubscriptionPartitionOffset offset);
}
