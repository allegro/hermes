package pl.allegro.tech.hermes.common.query;

import java.util.Collection;
import java.util.stream.Stream;

public interface Query<T> {

    Stream<T> filter(Stream<T> input);

    default Stream<T> filter(Collection<T> input) {
        return filter(input.stream());
    }
}
