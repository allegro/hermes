package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import pl.allegro.tech.hermes.api.SubscriptionName;

class SubscriptionProfiles {

  static final SubscriptionProfiles EMPTY = new SubscriptionProfiles(Map.of(), Instant.MIN);

  private final Map<SubscriptionName, SubscriptionProfile> profiles;
  private final Instant updateTimestamp;

  SubscriptionProfiles(
      Map<SubscriptionName, SubscriptionProfile> profiles, Instant updateTimestamp) {
    this.profiles = profiles;
    this.updateTimestamp = updateTimestamp;
  }

  Instant getUpdateTimestamp() {
    return updateTimestamp;
  }

  Set<SubscriptionName> getSubscriptions() {
    return profiles.keySet();
  }

  SubscriptionProfile getProfile(SubscriptionName subscriptionName) {
    return profiles.getOrDefault(subscriptionName, SubscriptionProfile.UNDEFINED);
  }

  Map<SubscriptionName, SubscriptionProfile> getProfiles() {
    return profiles;
  }
}
