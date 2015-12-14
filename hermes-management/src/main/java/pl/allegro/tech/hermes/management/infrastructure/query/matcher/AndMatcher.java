package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

import java.util.Collection;

public class AndMatcher<T> implements Matcher<T> {

    private Collection<Matcher<T>> matchers;

    public AndMatcher(Collection<Matcher<T>> matchers) {
        this.matchers = matchers;
    }

    @Override
    public boolean match(T value) {
        return matchers.stream().reduce(
                true,
                (match, matcher) -> match && matcher.match(value),
                (first, second) -> first && second
        );
    }
}
