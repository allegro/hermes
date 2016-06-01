package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;

public interface ProtocolMessageSenderProvider {

    MessageSender create(Subscription subscription);

    void start() throws Exception;

    void stop() throws Exception;
}
