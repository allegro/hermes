package pl.allegro.tech.hermes.management.domain.clients;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

import java.util.List;
import java.util.stream.Collectors;

public class AllTopicClientsServiceImplementation implements AllTopicClientsService{

    private final SubscriptionRepository subscriptionRepository;

    public AllTopicClientsServiceImplementation(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public List<String> getAllClientsByTopic(TopicName topicName) {
        return subscriptionRepository.listSubscriptions(topicName)
                .stream().map(e -> e.getOwner().getId()).collect(Collectors.toList());
    }
}
