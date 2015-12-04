package pl.allegro.tech.hermes.management.infrastructure.query;

import pl.allegro.tech.hermes.common.query.Query;
import pl.allegro.tech.hermes.management.infrastructure.query.matcher.Matcher;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class MatcherQuery<T> implements Query<T> {

    private final Matcher<T> matcher;

    private MatcherQuery(Matcher<T> matcher) {
        this.matcher = matcher;
    }

    @Override
    public Stream<T> filter(Stream<T> input) {
        return input.filter(getPredicate());
    }

    public Predicate<T> getPredicate() {
        return matcher::match;
    }

    public static <T> Query<T> fromMatcher(Matcher<T> matcher) {
        return new MatcherQuery<>(matcher);
    }
}
