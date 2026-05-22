package pl.allegro.tech.hermes.benchmark.consumer;

import java.util.Set;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.ResilientMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;

public class InMemoryProtocolMessageSenderProvider implements ProtocolMessageSenderProvider {

  private final InMemoryDelayedMessageSender messageSender;

  public InMemoryProtocolMessageSenderProvider(InMemoryDelayedMessageSender messageSender) {
    this.messageSender = messageSender;
  }

  @Override
  public MessageSender create(
      Subscription subscription, ResilientMessageSender resilientMessageSender) {
    return messageSender;
  }

  @Override
  public Set<String> getSupportedProtocols() {
    return Set.of("http", "https");
  }

  @Override
  public void start() {}

  @Override
  public void stop() {}
}
