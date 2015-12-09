package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

public class NotMatcher<T> implements Matcher<T> {

    private final Matcher<T> matcher;

    public NotMatcher(Matcher<T> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean match(T value) {
        return !matcher.match(value);
    }
}
