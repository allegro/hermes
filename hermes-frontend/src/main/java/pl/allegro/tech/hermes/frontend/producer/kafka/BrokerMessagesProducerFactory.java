package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducer;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;

import javax.inject.Inject;

public class BrokerMessagesProducerFactory implements Factory<BrokerMessagesProducer> {
    private final BrokerMessageProducer brokerMessageProducer;

    @Inject
    public BrokerMessagesProducerFactory(BrokerMessageProducer brokerMessageProducer) {
        this.brokerMessageProducer = brokerMessageProducer;
    }

    @Override
    public BrokerMessagesProducer provide() {
        return new KafkaMessagesProducer(brokerMessageProducer);
    }

    @Override
    public void dispose(BrokerMessagesProducer instance) {

    }
}
