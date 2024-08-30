package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.util.Objects;
import java.util.Optional;

final class ConsumerRateInfo {

  private final String consumerId;
  private final Optional<MaxRate> maxRate;
  private final RateHistory history;

  ConsumerRateInfo(String consumerId, RateInfo rateInfo) {
    this.consumerId = consumerId;
    this.maxRate = rateInfo.getMaxRate();
    this.history = rateInfo.getRateHistory();
  }

  Optional<MaxRate> getMaxRate() {
    return maxRate;
  }

  RateHistory getHistory() {
    return history;
  }

  String getConsumerId() {
    return consumerId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsumerRateInfo that = (ConsumerRateInfo) o;
    return Objects.equals(consumerId, that.consumerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(consumerId);
  }

  @Override
  public String toString() {
    return "ConsumerRateInfo{"
        + "consumerId='"
        + consumerId
        + '\''
        + ", maxRate="
        + maxRate
        + ", history="
        + history
        + '}';
  }
}
