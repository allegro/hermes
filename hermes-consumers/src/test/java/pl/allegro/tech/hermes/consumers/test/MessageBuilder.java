package pl.allegro.tech.hermes.consumers.test;


import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

import java.util.Optional;

public final class MessageBuilder {

    private final String content;

    private MessageBuilder(String content) {
        this.content = content;
    }

    public static MessageBuilder message(String content) {
        return new MessageBuilder(content);
    }

    public Message build() {
        return new Message(Optional.of("213"), 123, 1, "whatever", content.getBytes(), Optional.of(123L), Optional.of(123L));
    }
}
