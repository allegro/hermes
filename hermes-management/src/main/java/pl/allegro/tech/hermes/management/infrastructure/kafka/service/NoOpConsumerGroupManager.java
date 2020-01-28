package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.management.domain.subscription.ConsumerGroupManager;

public class NoOpConsumerGroupManager implements ConsumerGroupManager {
    @Override
    public void createConsumerGroup(Subscription subscription) {
        // no operation
    }
}
