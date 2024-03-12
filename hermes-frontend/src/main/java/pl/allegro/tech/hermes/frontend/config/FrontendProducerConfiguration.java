package pl.allegro.tech.hermes.frontend.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.producer.BrokerLatencyReporter;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaBrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaHeaderFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageSenders;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageSendersFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.MessageToKafkaProducerRecordConverter;
import pl.allegro.tech.hermes.frontend.producer.kafka.ProducerMetadataLoadingJob;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;

import java.util.List;
import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;

@Configuration
@EnableConfigurationProperties({
        LocalMessageStorageProperties.class,
        SchemaProperties.class,
        KafkaHeaderNameProperties.class,
        KafkaProducerProperties.class,
        KafkaClustersProperties.class,
        HTTPHeadersProperties.class
})
public class FrontendProducerConfiguration {

    @Bean
    public BrokerMessageProducer kafkaBrokerMessageProducer(KafkaMessageSenders kafkaMessageSenders,
                                                            MetricsFacade metricsFacade,
                                                            MessageToKafkaProducerRecordConverter messageConverter) {
        return new KafkaBrokerMessageProducer(kafkaMessageSenders, metricsFacade, messageConverter);
    }

    @Bean
    public KafkaHeaderFactory kafkaHeaderFactory(KafkaHeaderNameProperties kafkaHeaderNameProperties,
                                                 HTTPHeadersProperties httpHeadersProperties) {
        return new KafkaHeaderFactory(kafkaHeaderNameProperties, httpHeadersProperties.getPropagationAsKafkaHeaders());
    }

    @Bean(destroyMethod = "close")
    public KafkaMessageSenders kafkaMessageSenders(KafkaProducerProperties kafkaProducerProperties,
                                                   KafkaMessageSendersFactory kafkaMessageSendersFactory) {
        return kafkaMessageSendersFactory.provide(kafkaProducerProperties);
    }

    @Bean(destroyMethod = "close")
    public KafkaMessageSendersFactory kafkaMessageSendersFactory(KafkaClustersProperties kafkaClustersProperties,
                                                                 KafkaProducerProperties kafkaProducerProperties,
                                                                 TopicLoadingProperties topicLoadingProperties,
                                                                 TopicsCache topicsCache,
                                                                 LocalMessageStorageProperties localMessageStorageProperties,
                                                                 DatacenterNameProvider datacenterNameProvider) {
        KafkaProperties kafkaProperties = kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
        List<KafkaParameters> remoteKafkaProperties = kafkaClustersProperties.toRemoteKafkaProperties(datacenterNameProvider);
        return new KafkaMessageSendersFactory(
                kafkaProperties,
                remoteKafkaProperties,
                createAdminClient(kafkaProperties),
                topicsCache,
                topicLoadingProperties.getMetadata().getRetryCount(),
                topicLoadingProperties.getMetadata().getRetryInterval(),
                topicLoadingProperties.getMetadata().getThreadPoolSize(),
                localMessageStorageProperties.getBufferedSizeBytes(),
                kafkaProducerProperties.getMetadataMaxAge()
        );
    }

    private static AdminClient createAdminClient(KafkaProperties kafkaProperties) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerList());
        props.put(SECURITY_PROTOCOL_CONFIG, DEFAULT_SECURITY_PROTOCOL);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, (int) kafkaProperties.getAdminRequestTimeout().toMillis());
        if (kafkaProperties.isAuthenticationEnabled()) {
            props.put(SASL_MECHANISM, kafkaProperties.getAuthenticationMechanism());
            props.put(SECURITY_PROTOCOL_CONFIG, kafkaProperties.getAuthenticationProtocol());
            props.put(SASL_JAAS_CONFIG, kafkaProperties.getJaasConfig());
        }
        return AdminClient.create(props);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public ProducerMetadataLoadingJob producerMetadataLoadingJob(KafkaMessageSenders kafkaMessageSenders,
                                                                 TopicLoadingProperties topicLoadingProperties) {
        return new ProducerMetadataLoadingJob(
                kafkaMessageSenders,
                topicLoadingProperties.getMetadataRefreshJob().isEnabled(),
                topicLoadingProperties.getMetadataRefreshJob().getInterval()
        );
    }

    @Bean
    public MessageToKafkaProducerRecordConverter messageToKafkaProducerRecordConverter(KafkaHeaderFactory kafkaHeaderFactory,
                                                                                       SchemaProperties schemaProperties) {
        return new MessageToKafkaProducerRecordConverter(kafkaHeaderFactory, schemaProperties.isIdHeaderEnabled());
    }
}
