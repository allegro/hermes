package pl.allegro.tech.hermes.common.filtering;

import pl.allegro.tech.hermes.common.message.MessageContent;

import java.util.function.Predicate;

public class MessageFilter implements Predicate<MessageContent> {

    private String type;
    private Predicate<MessageContent> predicate;

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
