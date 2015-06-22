package pl.allegro.tech.hermes.consumers.consumer.converter;

import pl.allegro.tech.hermes.consumers.consumer.Message;

public class NoOperationMessageConverter implements MessageConverter {
    @Override
    public Message convert(Message message) {
        return message;
    }
}
