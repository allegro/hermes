package pl.allegro.tech.hermes.frontend.config;

import jakarta.inject.Named;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.BrokerTopicMetadataFetcher;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaBrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaBrokerTopicMetadataFetcher;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaHeaderFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageProducerFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.MessageToKafkaProducerRecordConverter;
import pl.allegro.tech.hermes.frontend.producer.kafka.Producers;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;

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
    public BrokerMessageProducer kafkaBrokerMessageProducer(Producers producers,
                                                            MetricsFacade metricsFacade,
                                                            MessageToKafkaProducerRecordConverter messageConverter) {
        return new KafkaBrokerMessageProducer(producers, metricsFacade, messageConverter);
    }

    @Bean
    public KafkaHeaderFactory kafkaHeaderFactory(KafkaHeaderNameProperties kafkaHeaderNameProperties,
                                                 HTTPHeadersProperties httpHeadersProperties) {
        return new KafkaHeaderFactory(kafkaHeaderNameProperties, httpHeadersProperties.getPropagationAsKafkaHeaders());
    }

    @Bean(destroyMethod = "close")
    public Producers kafkaMessageProducer(KafkaClustersProperties kafkaClustersProperties,
                                          KafkaProducerProperties kafkaProducerProperties,
                                          LocalMessageStorageProperties localMessageStorageProperties,
                                          DatacenterNameProvider datacenterNameProvider) {
        KafkaProperties kafkaProperties = kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
        return new KafkaMessageProducerFactory(kafkaProperties, kafkaProducerProperties,
                localMessageStorageProperties.getBufferedSizeBytes()).provide();
    }

    @Bean
    public MessageToKafkaProducerRecordConverter messageToKafkaProducerRecordConverter(KafkaHeaderFactory kafkaHeaderFactory,
                                                                                       SchemaProperties schemaProperties) {
        return new MessageToKafkaProducerRecordConverter(kafkaHeaderFactory, schemaProperties.isIdHeaderEnabled());
    }

    @Bean(destroyMethod = "close")
    public BrokerTopicMetadataFetcher topicAvailabilityChecker(@Named("kafkaMessageProducer") Producers producers,
                                                               KafkaProducerProperties kafkaProducerProperties,
                                                               KafkaClustersProperties kafkaClustersProperties,
                                                               DatacenterNameProvider datacenterNameProvider) {
        KafkaProperties kafkaProperties = kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerList());
        props.put(SECURITY_PROTOCOL_CONFIG, DEFAULT_SECURITY_PROTOCOL);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, kafkaProperties.getAdminRequestTimeout().toMillis());
        if (kafkaProperties.isAuthenticationEnabled()) {
            props.put(SASL_MECHANISM, kafkaProperties.getAuthenticationMechanism());
            props.put(SECURITY_PROTOCOL_CONFIG, kafkaProperties.getAuthenticationProtocol());
            props.put(SASL_JAAS_CONFIG, kafkaProperties.getJaasConfig());
        }
        AdminClient adminClient = AdminClient.create(props);
        return new KafkaBrokerTopicMetadataFetcher(producers,  adminClient, kafkaProducerProperties.getMetadataMaxAge());
    }
}
