package pl.allegro.tech.hermes.consumers.consumer.sender.pubsub;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;

public class PubSubMessageSenderProvider implements ProtocolMessageSenderProvider {
    @Override
    public MessageSender create(Subscription subscription) {
        return new PubSubMessageSender(subscription);
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }
}
