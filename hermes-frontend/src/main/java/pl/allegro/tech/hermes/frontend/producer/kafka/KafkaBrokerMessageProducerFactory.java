package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import javax.inject.Inject;

public class KafkaBrokerMessageProducerFactory implements Factory<KafkaBrokerMessageProducer> {

    private final Producers producers;
    private final HermesMetrics hermesMetrics;
    private final KafkaNamesMapper kafkaNamesMapper;

    @Inject
    public KafkaBrokerMessageProducerFactory(Producers producers, HermesMetrics hermesMetrics, KafkaNamesMapper kafkaNamesMapper) {
        this.producers = producers;
        this.hermesMetrics = hermesMetrics;
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    @Override
    public KafkaBrokerMessageProducer provide() {
        return new KafkaBrokerMessageProducer(producers, hermesMetrics, kafkaNamesMapper);
    }

    @Override
    public void dispose(KafkaBrokerMessageProducer instance) {

    }

}
