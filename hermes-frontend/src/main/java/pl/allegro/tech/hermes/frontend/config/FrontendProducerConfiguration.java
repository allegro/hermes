package pl.allegro.tech.hermes.frontend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaBrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaHeaderFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageProducerFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaTopicMetadataFetcher;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaTopicMetadataFetcherFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.Producers;

@Configuration
public class FrontendProducerConfiguration {

    @Bean
    public BrokerMessageProducer kafkaBrokerMessageProducer(Producers producers,
                                                            KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher,
                                                            HermesMetrics hermesMetrics,
                                                            KafkaHeaderFactory kafkaHeaderFactory,
                                                            ConfigFactory configFactory) {
        return new KafkaBrokerMessageProducer(producers, kafkaTopicMetadataFetcher, hermesMetrics, kafkaHeaderFactory,
                configFactory);
    }

    @Bean
    public KafkaHeaderFactory kafkaHeaderFactory(ConfigFactory configFactory) {
        return new KafkaHeaderFactory(configFactory);
    }

    @Bean(destroyMethod = "close")
    public Producers kafkaMessageProducer(ConfigFactory configFactory) {
        return new KafkaMessageProducerFactory(configFactory).provide();
    }

    @Bean(destroyMethod = "close")
    public KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher(ConfigFactory configFactory) {
        return new KafkaTopicMetadataFetcherFactory(configFactory).provide();
    }
}