package pl.allegro.tech.hermes.management.config.subscription.consumergroup;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupCleanUpService;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupToDeleteRepository;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperRepositoryManager;

@Configuration
@EnableConfigurationProperties(ConsumerGroupCleanUpProperties.class)
@ConditionalOnProperty(value = "consumer-group-clean-up.enabled", havingValue = "true")
public class ConsumerGroupCleanUpConfig {

    @Autowired
    ZookeeperRepositoryManager zookeeperRepositoryManager;

    @Bean
    ConsumerGroupCleanUpService consumerGroupCleanUpService(
            MultiDCAwareService multiDCAwareService,
            SubscriptionService subscriptionService,
            ConsumerGroupCleanUpProperties properties,
            Clock clock
    ) {
        return new ConsumerGroupCleanUpService(
                multiDCAwareService,
                zookeeperRepositoryManager.getRepositoriesByType(ConsumerGroupToDeleteRepository.class),
                subscriptionService,
                properties,
                clock
        );
    }
}
