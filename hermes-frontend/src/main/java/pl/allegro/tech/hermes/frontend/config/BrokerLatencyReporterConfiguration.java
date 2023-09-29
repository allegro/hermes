package pl.allegro.tech.hermes.frontend.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.producer.BrokerLatencyReporter;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaPartitionLeaderLoadingJob;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaPartitionLeaderRegistry;

import java.util.Properties;

@Configuration
@EnableConfigurationProperties(BrokerLatencyReporterProperties.class)
public class BrokerLatencyReporterConfiguration {
    @Bean
    KafkaPartitionLeaderRegistry kafkaPartitionLeaderRegistry(TopicsCache topicsCache, AdminClient adminClient) {
        return new KafkaPartitionLeaderRegistry(topicsCache, adminClient);
    }

    @Bean
    BrokerLatencyReporter brokerLatencyReporter(BrokerLatencyReporterProperties properties,
                                                MetricsFacade metricsFacade,
                                                KafkaPartitionLeaderRegistry leaderRegistry) {
        return new BrokerLatencyReporter(
                properties.isPerBrokerLatencyReportingEnabled(),
                metricsFacade,
                leaderRegistry,
                properties.getSlowResponseLoggingThreshold()
        );
    }

    @Bean
    KafkaPartitionLeaderLoadingJob leaderRegistryLoadingJob(
            KafkaPartitionLeaderRegistry leaderRegistry,
            BrokerLatencyReporterProperties properties
    ) {
        return new KafkaPartitionLeaderLoadingJob(
                leaderRegistry, properties.getKafkaPartitionLeaderRefreshInterval()
        );
    }

    @Bean
    AdminClient brokerAdminClient() {
        Properties props = new Properties();
        return AdminClient.create(props);
    }
}
