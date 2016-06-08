package pl.allegro.tech.hermes.consumers.consumer;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;

import javax.inject.Inject;

public class ActiveConsumerCounter {

    private final CounterStorage counterStorage;

    @Inject
    public ActiveConsumerCounter(CounterStorage counterStorage) {
        this.counterStorage = counterStorage;
    }

    public int countActiveConsumers(Subscription subscription) {
        // This is an ad-hoc implementation, utilizing exising inflight nodes.
        return counterStorage.countInflightNodes(subscription.getTopicName(), subscription.getName());
    }
}
