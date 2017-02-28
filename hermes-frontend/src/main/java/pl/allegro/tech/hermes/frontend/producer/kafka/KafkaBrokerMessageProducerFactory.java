package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import javax.inject.Inject;

public class KafkaBrokerMessageProducerFactory implements Factory<KafkaBrokerMessageProducer> {

    private final Producers producers;
    private final HermesMetrics hermesMetrics;

    @Inject
    public KafkaBrokerMessageProducerFactory(Producers producers, HermesMetrics hermesMetrics) {
        this.producers = producers;
        this.hermesMetrics = hermesMetrics;
    }

    @Override
    public KafkaBrokerMessageProducer provide() {
        return new KafkaBrokerMessageProducer(producers, hermesMetrics);
    }

    @Override
    public void dispose(KafkaBrokerMessageProducer instance) {

    }

}
