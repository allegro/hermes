package pl.allegro.tech.hermes.domain.filtering;

import java.util.function.Predicate;

public class MessageFilter implements Predicate<Filterable> {
    private final String type;
    private final Predicate<Filterable> predicate;

    public MessageFilter(String type, Predicate<Filterable> predicate) {
        this.type = type;
        this.predicate = predicate;
    }

    @Override
    public boolean test(Filterable message) {
        return predicate.test(message);
    }

    public String getType() {
        return type;
    }
}
