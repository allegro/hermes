package pl.allegro.tech.hermes.frontend.producer.kafka;

import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public class BufferAwareBrokerMessageProducer implements BrokerMessageProducer {

    private final BrokerMessageProducer bufferedBrokerMessageProducer;
    private final BrokerMessageProducer unbufferedBrokerMessageProducer;

    public BufferAwareBrokerMessageProducer(BrokerMessageProducer bufferedBrokerMessageProducer, BrokerMessageProducer unbufferedBrokerMessageProducer) {
        this.bufferedBrokerMessageProducer = bufferedBrokerMessageProducer;
        this.unbufferedBrokerMessageProducer = unbufferedBrokerMessageProducer;
    }

    @Override
    public void send(Message message, CachedTopic topic, PublishingCallback callback) {
        if (topic.getTopic().isFallbackToRemoteDatacenterEnabled()) {
            this.unbufferedBrokerMessageProducer.send(message, topic, callback);
        } else {
            this.bufferedBrokerMessageProducer.send(message, topic, callback);
        }
    }

    @Override
    public boolean isTopicAvailable(CachedTopic topic) {
        return bufferedBrokerMessageProducer.isTopicAvailable(topic);
    }
}
