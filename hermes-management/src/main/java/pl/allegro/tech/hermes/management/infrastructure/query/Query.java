package pl.allegro.tech.hermes.management.infrastructure.query;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface Query<T> {

    Stream<T> filter(Collection<T> input);

    Predicate<T> toPredicate();
}
