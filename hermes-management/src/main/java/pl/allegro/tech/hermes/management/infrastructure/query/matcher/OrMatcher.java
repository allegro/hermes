package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

import java.util.Collection;

public class OrMatcher implements Matcher {

    private final Collection<Matcher> matchers;

    public OrMatcher(Collection<Matcher> matchers) {
        this.matchers = matchers;
    }

    @Override
    public boolean match(Object value) {
        return matchers.stream().reduce(
                false,
                (match, matcher) -> match || matcher.match(value),
                (first, second) -> first || second
        );
    }
}
