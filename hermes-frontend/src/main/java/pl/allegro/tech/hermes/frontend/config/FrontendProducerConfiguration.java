package pl.allegro.tech.hermes.frontend.config;

import jakarta.inject.Named;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.producer.BrokerLatencyReporter;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.FallbackToRemoteDatacenterAwareMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaHeaderFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageSenders;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageSendersFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaTopicMetadataFetcher;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaTopicMetadataFetcherFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.LocalDatacenterMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.MessageToKafkaProducerRecordConverter;
import pl.allegro.tech.hermes.frontend.producer.kafka.MultiDatacenterMessageProducer;
import pl.allegro.tech.hermes.frontend.readiness.AdminReadinessService;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;

import java.util.List;

@Configuration
@EnableConfigurationProperties({
        LocalMessageStorageProperties.class,
        SchemaProperties.class,
        KafkaHeaderNameProperties.class,
        KafkaProducerProperties.class,
        FailFastKafkaProducerProperties.class,
        KafkaClustersProperties.class,
        HTTPHeadersProperties.class
})
public class FrontendProducerConfiguration {

    @Bean
    public BrokerMessageProducer kafkaBrokerMessageProducer(@Named("localDatacenterBrokerProducer") BrokerMessageProducer localDatacenterBrokerProducer,
                                                            @Named("multiDatacenterBrokerProducer") BrokerMessageProducer multiDatacenterBrokerProducer) {

        return new FallbackToRemoteDatacenterAwareMessageProducer(
                localDatacenterBrokerProducer,
                multiDatacenterBrokerProducer
        );
    }

    @Bean
    public BrokerMessageProducer localDatacenterBrokerProducer(
            @Named("kafkaMessageSenders") KafkaMessageSenders kafkaMessageSenders,
            KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher,
            MessageToKafkaProducerRecordConverter messageConverter
    ) {
        return new LocalDatacenterMessageProducer(kafkaMessageSenders, kafkaTopicMetadataFetcher, messageConverter);
    }

    @Bean
    public BrokerMessageProducer multiDatacenterBrokerProducer(
            @Named("failFastKafkaMessageSenders") KafkaMessageSenders kafkaMessageSenders,
            MessageToKafkaProducerRecordConverter messageConverter,
            FailFastKafkaProducerProperties kafkaProducerProperties,
            AdminReadinessService adminReadinessService
    ) {
        return new MultiDatacenterMessageProducer(
                kafkaMessageSenders,
                adminReadinessService,
                messageConverter,
                kafkaProducerProperties.getSpeculativeSendDelay()
        );
    }

    @Bean
    public KafkaHeaderFactory kafkaHeaderFactory(KafkaHeaderNameProperties kafkaHeaderNameProperties,
                                                 HTTPHeadersProperties httpHeadersProperties) {
        return new KafkaHeaderFactory(kafkaHeaderNameProperties, httpHeadersProperties.getPropagationAsKafkaHeaders());
    }

    @Bean(destroyMethod = "close")
    public KafkaMessageSenders kafkaMessageSenders(KafkaClustersProperties kafkaClustersProperties,
                                                   KafkaProducerProperties kafkaProducerProperties,
                                                   LocalMessageStorageProperties localMessageStorageProperties,
                                                   DatacenterNameProvider datacenterNameProvider,
                                                   BrokerLatencyReporter brokerLatencyReporter,
                                                   MetricsFacade metricsFacade
                                          ) {
        KafkaProperties kafkaProperties = kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
        List<KafkaProperties> remoteKafkaProperties = kafkaClustersProperties.toRemoteKafkaProperties(datacenterNameProvider);

        return new KafkaMessageSendersFactory(
                kafkaProperties,
                remoteKafkaProperties,
                kafkaProducerProperties,
                brokerLatencyReporter,
                metricsFacade,
                localMessageStorageProperties.getBufferedSizeBytes()
                ).provide("default");

    }

    @Bean(destroyMethod = "close")
    public KafkaMessageSenders failFastKafkaMessageSenders(KafkaClustersProperties kafkaClustersProperties,
                                                           FailFastKafkaProducerProperties kafkaProducerProperties,
                                                           LocalMessageStorageProperties localMessageStorageProperties,
                                                           DatacenterNameProvider datacenterNameProvider, BrokerLatencyReporter brokerLatencyReporter,
                                                           MetricsFacade metricsFacade
                                                           ) {
        KafkaProperties kafkaProperties = kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
        List<KafkaProperties> remoteKafkaProperties = kafkaClustersProperties.toRemoteKafkaProperties(datacenterNameProvider);
        return new KafkaMessageSendersFactory(
                kafkaProperties,
                remoteKafkaProperties,
                kafkaProducerProperties,
                brokerLatencyReporter,
                metricsFacade,
                localMessageStorageProperties.getBufferedSizeBytes()
        ).provide("failFast");

    }

    @Bean(destroyMethod = "close")
    public KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher(KafkaProducerProperties kafkaProducerProperties,
                                                               KafkaClustersProperties kafkaClustersProperties,
                                                               DatacenterNameProvider datacenterNameProvider) {
        KafkaProperties kafkaProperties = kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
        return new KafkaTopicMetadataFetcherFactory(kafkaProperties, kafkaProducerProperties.getMetadataMaxAge(),
                (int) kafkaProperties.getAdminRequestTimeout().toMillis()).provide();
    }

    @Bean
    public MessageToKafkaProducerRecordConverter messageToKafkaProducerRecordConverter(KafkaHeaderFactory kafkaHeaderFactory,
                                                                                       SchemaProperties schemaProperties) {
        return new MessageToKafkaProducerRecordConverter(kafkaHeaderFactory, schemaProperties.isIdHeaderEnabled());
    }
}
