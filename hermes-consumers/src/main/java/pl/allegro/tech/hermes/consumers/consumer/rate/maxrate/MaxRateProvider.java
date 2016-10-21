package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

public interface MaxRateProvider {
    double get();
    default void start() {}
    default void shutdown() {}
}
