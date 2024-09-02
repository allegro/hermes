package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;

class ConsumerRateHistory {

  private final Map<SubscriptionName, RateHistory> rateHistories;

  ConsumerRateHistory() {
    this.rateHistories = new HashMap<>();
  }

  RateHistory getRateHistory(SubscriptionName subscription) {
    return rateHistories.getOrDefault(subscription, RateHistory.empty());
  }

  void setRateHistory(SubscriptionName subscription, RateHistory rateHistory) {
    rateHistories.put(subscription, rateHistory);
  }

  void cleanup(Set<SubscriptionName> subscriptions) {
    rateHistories.entrySet().removeIf(entry -> !subscriptions.contains(entry.getKey()));
  }

  int size() {
    return rateHistories.size();
  }

  Map<SubscriptionId, RateHistory> toSubscriptionIdsMap(
      SubscriptionIdMapper subscriptionIdMapping) {
    return rateHistories.keySet().stream()
        .map(subscriptionIdMapping::mapToSubscriptionId)
        .filter(Optional::isPresent)
        .collect(
            Collectors.toMap(
                Optional::get,
                subscriptionId -> rateHistories.get(subscriptionId.get().getSubscriptionName())));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsumerRateHistory that = (ConsumerRateHistory) o;
    return Objects.equals(rateHistories, that.rateHistories);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rateHistories);
  }
}
