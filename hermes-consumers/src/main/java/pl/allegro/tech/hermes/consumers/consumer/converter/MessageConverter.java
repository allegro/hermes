package pl.allegro.tech.hermes.consumers.consumer.converter;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.Message;

public interface MessageConverter {

  Message convert(Message message, Topic topic);
}
