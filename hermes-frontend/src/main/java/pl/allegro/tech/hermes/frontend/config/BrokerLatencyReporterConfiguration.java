package pl.allegro.tech.hermes.frontend.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.producer.BrokerLatencyReporter;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaPartitionLeaderLoadingJob;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaPartitionLeaderRegistry;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;

import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;

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
    AdminClient brokerAdminClient(KafkaClustersProperties kafkaClustersProperties,
                                  DatacenterNameProvider datacenterNameProvider) {
        KafkaProperties kafkaProperties = kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerList());
        props.put(SECURITY_PROTOCOL_CONFIG, DEFAULT_SECURITY_PROTOCOL);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, (int) kafkaProperties.getAdminRequestTimeout().toMillis());
        if (kafkaProperties.isEnabled()) {
            props.put(SASL_MECHANISM, kafkaProperties.getMechanism());
            props.put(SECURITY_PROTOCOL_CONFIG, kafkaProperties.getProtocol());
            props.put(SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required\n"
                            + "username=\"" + kafkaProperties.getUsername() + "\"\n"
                            + "password=\"" + kafkaProperties.getPassword() + "\";"
            );
        }
        return AdminClient.create(props);
    }
}
