package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import pl.allegro.tech.hermes.api.Topic;

public interface MessageContentReaderFactory {
  MessageContentReader provide(Topic topic);
}
