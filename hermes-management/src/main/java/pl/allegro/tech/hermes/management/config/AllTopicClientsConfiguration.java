package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.domain.clients.AllTopicClientsService;
import pl.allegro.tech.hermes.management.domain.clients.DefaultAllTopicClientsService;

@Configuration
public class AllTopicClientsConfiguration {

    @Bean
    @ConditionalOnMissingBean(AllTopicClientsService.class)
    public AllTopicClientsService allTopicClientsService(SubscriptionRepository subscriptionRepository) {
        return new DefaultAllTopicClientsService(subscriptionRepository);
    }
}
