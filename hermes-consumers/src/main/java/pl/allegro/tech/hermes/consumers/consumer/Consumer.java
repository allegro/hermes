package pl.allegro.tech.hermes.consumers.consumer;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

import java.util.Set;

public interface Consumer {

    void consume(Runnable signalsInterrupt);

    void initialize();

    void prepareToTearDown();

    boolean isReadyToBeTornDown();

    void tearDown();

    void updateSubscription(Subscription subscription);

    void updateTopic(Topic topic);

    void commit(Set<SubscriptionPartitionOffset> offsets);

    boolean moveOffset(PartitionOffset subscriptionPartitionOffset);

    Subscription getSubscription();
}
