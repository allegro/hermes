package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.api.EndpointAddress;

public interface ProtocolMessageSenderProvider {

    MessageSender create(EndpointAddress endpoint);

    void start() throws Exception;

    void stop() throws Exception;
}
