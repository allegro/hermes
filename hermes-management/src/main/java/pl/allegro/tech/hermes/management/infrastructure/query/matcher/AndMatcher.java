package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

import java.util.Collection;

public class AndMatcher implements Matcher {

    private Collection<Matcher> matchers;

    public AndMatcher(Collection<Matcher> matchers) {
        this.matchers = matchers;
    }

    @Override
    public boolean match(Object value) {
        return matchers.stream().reduce(
                true,
                (match, matcher) -> match && matcher.match(value),
                (first, second) -> first && second
        );
    }
}
