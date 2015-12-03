package pl.allegro.tech.hermes.management.domain.query.matcher;

public interface MatcherFactory {

    <T> Matcher<T> createMatcher(String path, Object value);
}
