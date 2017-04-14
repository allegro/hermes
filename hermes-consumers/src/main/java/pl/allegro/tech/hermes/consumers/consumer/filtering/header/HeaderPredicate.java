package pl.allegro.tech.hermes.consumers.consumer.filtering.header;

import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.function.Predicate;
import java.util.regex.Pattern;

class HeaderPredicate implements Predicate<Message> {

    private final String name;
    private final Pattern valuePattern;

    HeaderPredicate(String name, Pattern valuePattern) {
        this.name = name;
        this.valuePattern = valuePattern;
    }

    @Override
    public boolean test(Message message) {
        return message.getExternalMetadata()
                .entrySet().stream()
                .filter(h -> h.getKey().equals(name))
                .findFirst()
                .filter(h -> valuePattern.matcher(h.getValue()).matches())
                .isPresent();
    }
}
