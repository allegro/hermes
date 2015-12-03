package pl.allegro.tech.hermes.management.domain.query.matcher;

public interface Matcher<T> {

    boolean match(T value);
}
