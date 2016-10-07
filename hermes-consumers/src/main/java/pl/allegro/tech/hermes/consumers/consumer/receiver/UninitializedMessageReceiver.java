package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

import java.util.Optional;
import java.util.Set;

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
    public void moveOffset(SubscriptionPartitionOffset offset) {
        throw new ConsumerNotInitializedException();
    }
}
