package pl.allegro.tech.hermes.management.infrastructure.utils;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Iterators {

  private Iterators() {}

  public static <T> Stream<T> stream(Iterator<? extends T> iterator) {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL), false);
  }
}
