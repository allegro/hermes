package pl.allegro.tech.hermes.management.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.domain.clients.AllTopicClientsService;
import pl.allegro.tech.hermes.management.domain.clients.OfflineClientsService;

import java.util.Optional;

@Configuration
public class AllTopicClientsConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AllTopicClientsConfiguration.class);

    @Bean(name = "allTopicClientsService")
    @ConditionalOnMissingBean(name = "allTopicClientsService")
    public AllTopicClientsService allTopicClientsService(Optional<OfflineClientsService> offlineClientsService, SubscriptionRepository subscriptionRepository) {
        logger.info("Creating allTopicClientsService bean");
        return new AllTopicClientsService(offlineClientsService, subscriptionRepository);
    }
}
