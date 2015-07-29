package pl.allegro.tech.hermes.frontend.publishing;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

import javax.inject.Inject;
import javax.inject.Singleton;

import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.SENDING_TO_KAFKA;
import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.SENDING_TO_KAFKA_PRODUCER_QUEUE;

@Singleton
public class MessagePublisher {

    private final BrokerMessageProducer brokerMessageProducer;

    @Inject
    public MessagePublisher(BrokerMessageProducer brokerMessageProducer) {
        this.brokerMessageProducer = brokerMessageProducer;
    }

    public void publish(Message message, Topic topic, MessageState messageState, PublishingCallback... callbacks) {
        messageState.setState(SENDING_TO_KAFKA_PRODUCER_QUEUE);
        brokerMessageProducer.send(message, topic, callbacks);
        messageState.setState(SENDING_TO_KAFKA);
    }
}
