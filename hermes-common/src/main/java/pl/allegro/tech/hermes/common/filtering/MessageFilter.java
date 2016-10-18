package pl.allegro.tech.hermes.common.filtering;

import pl.allegro.tech.hermes.common.message.MessageContent;

import java.util.function.Predicate;

public class MessageFilter implements Predicate<MessageContent> {
    private final String type;
    private final Predicate<MessageContent> predicate;

    public MessageFilter(String type, Predicate<MessageContent> predicate) {
        this.type = type;
        this.predicate = predicate;
    }

    @Override
    public boolean test(MessageContent message) {
        return predicate.test(message);
    }

    public String getType() {
        return type;
    }
}
