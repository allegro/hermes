package pl.allegro.tech.hermes.management.domain.clients;

import java.util.List;
import java.util.stream.Collectors;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

public class DefaultAllTopicClientsService implements AllTopicClientsService {

  private final SubscriptionRepository subscriptionRepository;

  public DefaultAllTopicClientsService(SubscriptionRepository subscriptionRepository) {
    this.subscriptionRepository = subscriptionRepository;
  }

  @Override
  public List<String> getAllClientsByTopic(TopicName topicName) {
    return subscriptionRepository.listSubscriptions(topicName).stream()
        .map(subscription -> subscription.getOwner().getId())
        .distinct()
        .collect(Collectors.toList());
  }
}
