package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;

public interface ProtocolMessageSenderProvider {

    @Deprecated
    default MessageSender create(EndpointAddress endpoint) {
        throw new UnsupportedOperationException();
    }

    default MessageSender create(Subscription subscription) {
        return create(subscription.getEndpoint());
    }

    void start() throws Exception;

    void stop() throws Exception;
}
