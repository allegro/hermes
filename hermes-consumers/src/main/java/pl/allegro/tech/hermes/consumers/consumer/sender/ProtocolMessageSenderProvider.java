package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.ResilientMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;

import java.util.Set;

public interface ProtocolMessageSenderProvider {

    MessageSender create(Subscription subscription, ResilientMessageSender resilientMessageSender, SubscriptionLoadRecorder loadRecorder);

    Set<String> getSupportedProtocols();

    void start() throws Exception;

    void stop() throws Exception;
}
