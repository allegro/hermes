package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.util.Optional;

class RateInfo {

  private static final RateInfo empty = new RateInfo(Optional.empty(), RateHistory.empty());

  private final Optional<MaxRate> maxRate;

  private final RateHistory rateHistory;

  RateInfo(Optional<MaxRate> maxRate, RateHistory rateHistory) {
    this.maxRate = maxRate;
    this.rateHistory = rateHistory;
  }

  static RateInfo empty() {
    return empty;
  }

  static RateInfo withNoHistory(MaxRate maxRate) {
    return new RateInfo(Optional.of(maxRate), RateHistory.empty());
  }

  static RateInfo withNoMaxRate(RateHistory rateHistory) {
    return new RateInfo(Optional.empty(), rateHistory);
  }

  Optional<MaxRate> getMaxRate() {
    return maxRate;
  }

  RateHistory getRateHistory() {
    return rateHistory;
  }

  RateInfo copyWithNewMaxRate(MaxRate maxRate) {
    return new RateInfo(Optional.of(maxRate), this.rateHistory);
  }

  RateInfo copyWithNewRateHistory(RateHistory rateHistory) {
    return new RateInfo(this.maxRate, rateHistory);
  }

  @Override
  public String toString() {
    return "RateInfo{" + "maxRate=" + maxRate + ", rateHistory=" + rateHistory + '}';
  }
}
