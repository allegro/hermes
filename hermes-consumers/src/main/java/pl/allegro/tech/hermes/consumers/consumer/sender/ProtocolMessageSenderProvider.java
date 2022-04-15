package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.api.Subscription;

import java.util.Set;

public interface ProtocolMessageSenderProvider {

    MessageSender create(Subscription subscription);

    Set<String> getSupportedProtocols();

    void start() throws Exception;

    void stop() throws Exception;
}
