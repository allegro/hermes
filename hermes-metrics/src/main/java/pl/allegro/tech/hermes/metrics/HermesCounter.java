package pl.allegro.tech.hermes.metrics;

public interface HermesCounter {
  void increment(long size);

  default void increment() {
    increment(1L);
  }
}
