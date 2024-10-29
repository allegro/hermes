package pl.allegro.tech.hermes.management.domain.subscription;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;

public interface ConsumerGroupManager {

  void createConsumerGroup(Topic topic, Subscription subscription);
}
