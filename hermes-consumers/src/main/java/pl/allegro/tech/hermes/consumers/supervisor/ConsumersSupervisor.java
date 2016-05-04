package pl.allegro.tech.hermes.consumers.supervisor;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;

public interface ConsumersSupervisor {
    void assignConsumerForSubscription(Subscription subscription);

    void deleteConsumerForSubscriptionName(SubscriptionName subscription);

    void updateSubscription(Subscription subscription);

    void shutdown() throws InterruptedException;

    void retransmit(SubscriptionName subscription) throws Exception;

    void restartConsumer(SubscriptionName subscription) throws Exception;

    void start() throws Exception;
}
