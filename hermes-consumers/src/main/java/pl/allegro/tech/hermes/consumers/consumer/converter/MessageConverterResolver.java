package pl.allegro.tech.hermes.consumers.consumer.converter;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;

public interface MessageConverterResolver {
  MessageConverter converterFor(Message message, Subscription subscription);
}
