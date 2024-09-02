package pl.allegro.tech.hermes.consumers.consumer.sender;

import java.util.Set;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.ResilientMessageSender;

public interface ProtocolMessageSenderProvider {

  MessageSender create(Subscription subscription, ResilientMessageSender resilientMessageSender);

  Set<String> getSupportedProtocols();

  void start() throws Exception;

  void stop() throws Exception;
}
