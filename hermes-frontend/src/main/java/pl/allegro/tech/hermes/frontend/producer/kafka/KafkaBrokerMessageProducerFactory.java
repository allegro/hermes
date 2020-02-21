package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import javax.inject.Inject;

public class KafkaBrokerMessageProducerFactory implements Factory<KafkaBrokerMessageProducer> {

    private final Producers producers;
    private final HermesMetrics hermesMetrics;
    private final KafkaHeaderFactory kafkaHeaderFactory;

    @Inject
    public KafkaBrokerMessageProducerFactory(Producers producers,
                                             HermesMetrics hermesMetrics,
                                             KafkaHeaderFactory kafkaHeaderFactory) {
        this.producers = producers;
        this.hermesMetrics = hermesMetrics;
        this.kafkaHeaderFactory = kafkaHeaderFactory;
    }

    @Override
    public KafkaBrokerMessageProducer provide() {
        return new KafkaBrokerMessageProducer(producers, hermesMetrics, kafkaHeaderFactory);
    }

    @Override
    public void dispose(KafkaBrokerMessageProducer instance) {

    }

}
