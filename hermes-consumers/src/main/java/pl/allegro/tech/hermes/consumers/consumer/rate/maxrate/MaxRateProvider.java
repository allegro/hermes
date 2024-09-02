package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import pl.allegro.tech.hermes.api.Subscription;

public interface MaxRateProvider {
  double get();

  default void start() {}

  default void shutdown() {}

  default void updateSubscription(Subscription newSubscription) {}
}
