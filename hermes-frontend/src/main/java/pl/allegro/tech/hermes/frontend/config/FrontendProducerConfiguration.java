package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaBrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaHeaderFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageProducerFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaTopicMetadataFetcher;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaTopicMetadataFetcherFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.MessageToKafkaProducerRecordConverter;
import pl.allegro.tech.hermes.frontend.producer.kafka.Producers;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;

@Configuration
@EnableConfigurationProperties({
        LocalMessageStorageProperties.class,
        SchemaProperties.class,
        KafkaHeaderNameProperties.class,
        KafkaProducerProperties.class,
        KafkaClustersProperties.class
})
public class FrontendProducerConfiguration {

    @Bean
    public BrokerMessageProducer kafkaBrokerMessageProducer(Producers producers,
                                                            KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher,
                                                            HermesMetrics hermesMetrics,
                                                            MessageToKafkaProducerRecordConverter messageConverter) {
        return new KafkaBrokerMessageProducer(producers, kafkaTopicMetadataFetcher, hermesMetrics, messageConverter);
    }

    @Bean
    public KafkaHeaderFactory kafkaHeaderFactory(KafkaHeaderNameProperties kafkaHeaderNameProperties) {
        return new KafkaHeaderFactory(kafkaHeaderNameProperties);
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
