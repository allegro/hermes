package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.RateLimitingMessageSender;

import java.util.Set;

public interface ProtocolMessageSenderProvider {

    MessageSender create(Subscription subscription, RateLimitingMessageSender rateLimitingMessageSender);

    Set<String> getSupportedProtocols();

    void start() throws Exception;

    void stop() throws Exception;
}
