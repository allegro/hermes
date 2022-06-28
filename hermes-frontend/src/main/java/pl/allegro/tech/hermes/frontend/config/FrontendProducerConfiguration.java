package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.Producers;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaTopicMetadataFetcher;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaBrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaHeaderFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageProducerFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaTopicMetadataFetcherFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.MessageToKafkaProducerRecordConverter;

@Configuration
@EnableConfigurationProperties({
        LocalMessageStorageProperties.class,
        SchemaProperties.class,
        KafkaHeaderNameProperties.class,
        KafkaProducerProperties.class
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
        return new KafkaHeaderFactory(kafkaHeaderNameProperties.toKafkaHeaderNameParameters());
    }

    @Bean(destroyMethod = "close")
    public Producers kafkaMessageProducer(ConfigFactory configFactory,
                                          KafkaProducerProperties kafkaProducerProperties,
                                          LocalMessageStorageProperties localMessageStorageProperties) {
        return new KafkaMessageProducerFactory(configFactory, kafkaProducerProperties.toKafkaProducerParameters(), localMessageStorageProperties.getBufferedSizeBytes()).provide();
    }

    @Bean(destroyMethod = "close")
    public KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher(ConfigFactory configFactory, KafkaProducerProperties kafkaProducerProperties) {
        return new KafkaTopicMetadataFetcherFactory(configFactory, kafkaProducerProperties.getMetadataMaxAge()).provide();
    }

    @Bean
    public MessageToKafkaProducerRecordConverter messageToKafkaProducerRecordConverter(KafkaHeaderFactory kafkaHeaderFactory, SchemaProperties schemaProperties) {
        return new MessageToKafkaProducerRecordConverter(kafkaHeaderFactory, schemaProperties.isIdHeaderEnabled());
    }
}
