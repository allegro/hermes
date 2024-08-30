package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.Counter;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.metrics.counters.DefaultHermesCounter;

public class SubscriptionHermesCounter extends DefaultHermesCounter {

  private final SubscriptionName subscription;

  private SubscriptionHermesCounter(Counter micrometerCounter, SubscriptionName subscription) {
    super(micrometerCounter);
    this.subscription = subscription;
  }

  public static SubscriptionHermesCounter from(
      Counter micrometerCounter, SubscriptionName subscription) {
    return new SubscriptionHermesCounter(micrometerCounter, subscription);
  }

  SubscriptionName getSubscription() {
    return subscription;
  }

  Counter getMicrometerCounter() {
    return micrometerCounter;
  }
}
