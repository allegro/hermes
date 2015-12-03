package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

public interface MatcherFactory {

    <T> Matcher<T> createMatcher(String path, Object value);
}
