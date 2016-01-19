package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

public class NotMatcher implements Matcher {

    private final Matcher matcher;

    public NotMatcher(Matcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean match(Object value) {
        return !matcher.match(value);
    }
}
