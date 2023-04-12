package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaBrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaHeaderFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageProducerFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.MessageToKafkaProducerRecordConverter;
import pl.allegro.tech.hermes.frontend.producer.kafka.Producers;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;

@Configuration
@EnableConfigurationProperties({
        LocalMessageStorageProperties.class,
        SchemaProperties.class,
        KafkaHeaderNameProperties.class,
        KafkaProducerProperties.class,
        KafkaClustersProperties.class,
        ReadinessCheckProperties.class
})
public class FrontendProducerConfiguration {

    @Bean
    public BrokerMessageProducer kafkaBrokerMessageProducer(Producers producers,
                                                            ReadinessCheckProperties properties,
                                                            HermesMetrics hermesMetrics,
                                                            MessageToKafkaProducerRecordConverter messageConverter) {
        return new KafkaBrokerMessageProducer(
                producers,
                hermesMetrics,
                messageConverter,
                properties.getMinInSyncReplicasAckLeader(),
                properties.getMinInSyncReplicasAckAll());
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

    @Bean
    public MessageToKafkaProducerRecordConverter messageToKafkaProducerRecordConverter(KafkaHeaderFactory kafkaHeaderFactory,
                                                                                       SchemaProperties schemaProperties) {
        return new MessageToKafkaProducerRecordConverter(kafkaHeaderFactory, schemaProperties.isIdHeaderEnabled());
    }
}
