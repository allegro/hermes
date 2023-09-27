package pl.allegro.tech.hermes.consumers.consumer;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

import java.util.Set;

public interface Consumer {

    /**
     * Consume **must** make sure that interrupted status is restored as it is needed for stopping unhealthy consumers.
     * Swallowing the interrupt by consume or any of its dependencies will result in consumer being marked
     * as unhealthy and will prevent commits despite messages being sent to subscribers.
     */
    void consume(Runnable signalsInterrupt);

    void initialize();

    void tearDown();

    void updateSubscription(Subscription subscription);

    void updateTopic(Topic topic);

    void commit(Set<SubscriptionPartitionOffset> offsets);

    boolean moveOffset(PartitionOffset subscriptionPartitionOffset);

    Subscription getSubscription();
}
