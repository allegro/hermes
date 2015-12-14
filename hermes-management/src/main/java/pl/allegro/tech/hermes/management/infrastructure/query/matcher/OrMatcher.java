package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

import java.util.Collection;

public class OrMatcher<T> implements Matcher<T> {

    private final Collection<Matcher<T>> matchers;

    public OrMatcher(Collection<Matcher<T>> matchers) {
        this.matchers = matchers;
    }

    @Override
    public boolean match(T value) {
        return matchers.stream().reduce(
                false,
                (match, matcher) -> match || matcher.match(value),
                (first, second) -> first || second
        );
    }
}
