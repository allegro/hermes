package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

public interface Matcher<T> {

    boolean match(T value);
}
