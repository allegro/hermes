package pl.allegro.tech.hermes.consumers.consumer.filtering;

import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.function.Predicate;

public class MessageFilter implements Predicate<Message> {

    private String type;
    private Predicate<Message> predicate;

    public MessageFilter(String type, Predicate<Message> predicate) {
        this.type = type;
        this.predicate = predicate;
    }

    @Override
    public boolean test(Message message) {
        return predicate.test(message);
    }

    public String getType() {
        return type;
    }
}
