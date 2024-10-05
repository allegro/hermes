package pl.allegro.tech.hermes.api;

import java.util.Collection;
import java.util.stream.Stream;

public interface Query<T> {

  Stream<T> filter(Stream<T> input);

  default Stream<T> filter(Collection<T> input) {
    return filter(input.stream());
  }

  <K> Stream<K> filterNames(Stream<K> input);

  default <K> Stream<K> filterNames(Collection<K> input) {
    return filterNames(input.stream());
  }
}
