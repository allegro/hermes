package pl.allegro.tech.hermes.management.domain.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.api.OfflineClientsEndpoint;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AllTopicClientsServiceImplementation implements AllTopicClientsService{

    private static final Logger logger = LoggerFactory.getLogger(OfflineClientsEndpoint.class);

    private final Optional<OfflineClientsService> offlineClientsService;
    private final SubscriptionRepository subscriptionRepository;

    public AllTopicClientsServiceImplementation(Optional<OfflineClientsService> offlineClientsService, SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
        if (!offlineClientsService.isPresent()) {
            logger.info("Offline clients bean is absent");
        }
        this.offlineClientsService = offlineClientsService;
    }

    public List<String> getAllClientsByTopic(TopicName topicName) {
        List<String> topicClients = subscriptionRepository.listSubscriptions(topicName)
                .stream().map(e -> e.getOwner().getId()).collect(Collectors.toList());

        if(offlineClientsService.isPresent()) {
            List<List<String>> offlineTopicClients = offlineClientsService.get().find(topicName)
                    .stream().map(OfflineClient::getOwners).collect(Collectors.toList());
            offlineTopicClients.forEach(topicClients::addAll);
        }

        return topicClients.stream().distinct().collect(Collectors.toList());
    }
}
