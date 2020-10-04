package pl.allegro.tech.hermes.domain.filtering.header;

import pl.allegro.tech.hermes.domain.filtering.Filterable;

import java.util.function.Predicate;
import java.util.regex.Pattern;

class HeaderPredicate implements Predicate<Filterable> {

    private final String name;
    private final Pattern valuePattern;

    HeaderPredicate(String name, Pattern valuePattern) {
        this.name = name;
        this.valuePattern = valuePattern;
    }

    @Override
    public boolean test(Filterable message) {
        return message.getExternalMetadata()
                .entrySet().stream()
                .filter(h -> h.getKey().equals(name))
                .findFirst()
                .filter(h -> valuePattern.matcher(h.getValue()).matches())
                .isPresent();
    }
}
