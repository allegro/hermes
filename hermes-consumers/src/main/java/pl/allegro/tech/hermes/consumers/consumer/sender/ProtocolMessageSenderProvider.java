package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.SendFutureProvider;

import java.util.Set;

public interface ProtocolMessageSenderProvider {

    MessageSender create(Subscription subscription, SendFutureProvider sendFutureProvider);

    Set<String> getSupportedProtocols();

    void start() throws Exception;

    void stop() throws Exception;
}
