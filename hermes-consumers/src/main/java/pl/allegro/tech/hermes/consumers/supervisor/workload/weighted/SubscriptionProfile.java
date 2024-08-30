package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import java.time.Instant;

class SubscriptionProfile {

  static SubscriptionProfile UNDEFINED = new SubscriptionProfile(Instant.MIN, Weight.ZERO);

  private final Instant lastRebalanceTimestamp;
  private final Weight weight;

  SubscriptionProfile(Instant lastRebalanceTimestamp, Weight weight) {
    this.lastRebalanceTimestamp = lastRebalanceTimestamp;
    this.weight = weight;
  }

  Weight getWeight() {
    return weight;
  }

  Instant getLastRebalanceTimestamp() {
    return lastRebalanceTimestamp;
  }
}
