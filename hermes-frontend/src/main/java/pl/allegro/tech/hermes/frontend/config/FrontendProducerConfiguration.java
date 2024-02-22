package pl.allegro.tech.hermes.frontend.config;

import jakarta.inject.Named;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.BufferAwareBrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaBrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaHeaderFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageProducerFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaTopicMetadataFetcher;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaTopicMetadataFetcherFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.MessageToKafkaProducerRecordConverter;
import pl.allegro.tech.hermes.frontend.producer.kafka.MultiDCKafkaBrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.Producers;
import pl.allegro.tech.hermes.frontend.producer.kafka.SimpleRemoteProducerProvider;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;

import java.util.List;

@Configuration
@EnableConfigurationProperties({
        LocalMessageStorageProperties.class,
        SchemaProperties.class,
        KafkaHeaderNameProperties.class,
        KafkaProducerProperties.class,
        ExperimentalKafkaProducerProperties.class,
        KafkaClustersProperties.class,
        HTTPHeadersProperties.class
})
public class FrontendProducerConfiguration {

    @Bean
    public BrokerMessageProducer kafkaBrokerMessageProducer(@Named("bufferedMessageBrokerProducer") BrokerMessageProducer bufferedMessageBrokerProducer,
                                                            @Named("unbufferedMessageBrokerProducer") BrokerMessageProducer unbufferedMessageBrokerProducer) {

        return new BufferAwareBrokerMessageProducer(
                bufferedMessageBrokerProducer,
                unbufferedMessageBrokerProducer
        );
    }

    @Bean
    public BrokerMessageProducer bufferedMessageBrokerProducer(
            @Named("kafkaMessageProducer") Producers producers,
            KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher,
            MetricsFacade metricsFacade,
            MessageToKafkaProducerRecordConverter messageConverter
    ) {
        return new KafkaBrokerMessageProducer(producers, kafkaTopicMetadataFetcher, metricsFacade, messageConverter);
    }

    @Bean
    public BrokerMessageProducer unbufferedMessageBrokerProducer(
            @Named("experimentalKafkaMessageProducer") Producers producers,
            MetricsFacade metricsFacade,
            MessageToKafkaProducerRecordConverter messageConverter,
            ExperimentalKafkaProducerProperties kafkaProducerProperties
    ) {
        return new MultiDCKafkaBrokerMessageProducer(producers, new SimpleRemoteProducerProvider(), metricsFacade, messageConverter, kafkaProducerProperties.getSpeculativeSendDelay());
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
        List<KafkaProperties> remoteKafkaProperties = kafkaClustersProperties.toRemoteKafkaProperties(datacenterNameProvider);
        return new KafkaMessageProducerFactory(
                kafkaProperties,
                remoteKafkaProperties,
                kafkaProducerProperties,
                localMessageStorageProperties.getBufferedSizeBytes()).provide();

    }

    @Bean(destroyMethod = "close")
    public Producers experimentalKafkaMessageProducer(KafkaClustersProperties kafkaClustersProperties,
                                          ExperimentalKafkaProducerProperties kafkaProducerProperties,
                                          LocalMessageStorageProperties localMessageStorageProperties,
                                          DatacenterNameProvider datacenterNameProvider) {
        KafkaProperties kafkaProperties = kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
        List<KafkaProperties> remoteKafkaProperties = kafkaClustersProperties.toRemoteKafkaProperties(datacenterNameProvider);
        return new KafkaMessageProducerFactory(
                kafkaProperties,
                remoteKafkaProperties,
                kafkaProducerProperties,
                localMessageStorageProperties.getBufferedSizeBytes()).provide();

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
