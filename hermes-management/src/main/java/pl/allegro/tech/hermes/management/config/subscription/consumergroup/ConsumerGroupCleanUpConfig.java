package pl.allegro.tech.hermes.management.config.subscription.consumergroup;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupCleanUpScheduler;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupToDeleteRepository;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.management.infrastructure.leader.ManagementLeadership;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperRepositoryManager;

@Configuration
@EnableConfigurationProperties(ConsumerGroupCleanUpProperties.class)
public class ConsumerGroupCleanUpConfig {

  @Autowired ZookeeperRepositoryManager zookeeperRepositoryManager;

  @Bean
  ConsumerGroupCleanUpScheduler consumerGroupCleanUpScheduler(
      MultiDCAwareService multiDCAwareService,
      SubscriptionService subscriptionService,
      ConsumerGroupCleanUpProperties properties,
      ManagementLeadership managementLeadership,
      Clock clock) {
    return new ConsumerGroupCleanUpScheduler(
        multiDCAwareService,
        zookeeperRepositoryManager.getRepositoriesByType(ConsumerGroupToDeleteRepository.class),
        subscriptionService,
        properties,
        managementLeadership,
        clock);
  }
}
