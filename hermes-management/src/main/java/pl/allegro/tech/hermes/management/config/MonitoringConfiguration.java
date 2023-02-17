package pl.allegro.tech.hermes.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.config.kafka.KafkaClustersProperties;
import pl.allegro.tech.hermes.management.config.kafka.KafkaNamesMappers;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.infrastructure.monitoring.MonitoringCache;

@Configuration
public class MonitoringConfiguration {

    @Bean
    MonitoringCache monitoringCache(KafkaClustersProperties kafkaClustersProperties,
                                    KafkaNamesMappers kafkaNamesMappers,
                                    MonitoringProperties monitoringProperties,
                                    SubscriptionService subscriptionService,
                                    TopicService topicService) {
        return new MonitoringCache(kafkaClustersProperties, kafkaNamesMappers, monitoringProperties, subscriptionService, topicService);
    }
}
