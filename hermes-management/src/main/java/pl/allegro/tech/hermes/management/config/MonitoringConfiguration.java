package pl.allegro.tech.hermes.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.config.kafka.KafkaClustersProperties;
import pl.allegro.tech.hermes.management.config.kafka.KafkaNamesMappers;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.infrastructure.monitoring.MonitoringCache;
import pl.allegro.tech.hermes.management.infrastructure.monitoring.MonitoringServicesCreator;

@Configuration
public class MonitoringConfiguration {

    @Bean
    MonitoringServicesCreator monitoringServicesCreator(KafkaClustersProperties kafkaClustersProperties,
                                                        KafkaNamesMappers kafkaNamesMappers) {
        return new MonitoringServicesCreator(kafkaClustersProperties, kafkaNamesMappers);
    }

    @Bean
    MonitoringCache monitoringCache(MonitoringProperties monitoringProperties,
                                    SubscriptionService subscriptionService,
                                    TopicService topicService,
                                    MonitoringServicesCreator monitoringServicesCreator) {
        return new MonitoringCache(monitoringProperties, subscriptionService, topicService, monitoringServicesCreator);
    }
}
