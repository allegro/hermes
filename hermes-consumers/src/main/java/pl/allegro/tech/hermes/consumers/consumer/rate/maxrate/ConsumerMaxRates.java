package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;

class ConsumerMaxRates {

  private final Map<SubscriptionName, MaxRate> maxRates;

  ConsumerMaxRates() {
    this.maxRates = new ConcurrentHashMap<>();
  }

  void setAllMaxRates(ConsumerMaxRates newMaxRates) {
    maxRates.clear();
    maxRates.putAll(newMaxRates.maxRates);
  }

  Optional<MaxRate> getMaxRate(SubscriptionName subscription) {
    return Optional.ofNullable(maxRates.get(subscription));
  }

  void setMaxRate(SubscriptionName subscription, MaxRate maxRate) {
    maxRates.put(subscription, maxRate);
  }

  public void cleanup(Set<SubscriptionName> subscriptions) {
    this.maxRates.entrySet().removeIf(entry -> !subscriptions.contains(entry.getKey()));
  }

  int size() {
    return maxRates.size();
  }

  Map<SubscriptionId, MaxRate> toSubscriptionsIdsMap(SubscriptionIdMapper subscriptionIdMapping) {
    return maxRates.keySet().stream()
        .map(subscriptionIdMapping::mapToSubscriptionId)
        .filter(Optional::isPresent)
        .collect(
            Collectors.toMap(
                Optional::get,
                subscriptionId -> maxRates.get(subscriptionId.get().getSubscriptionName())));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsumerMaxRates that = (ConsumerMaxRates) o;
    return Objects.equals(maxRates, that.maxRates);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxRates);
  }
}
