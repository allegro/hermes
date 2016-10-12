package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.util.Objects;
import java.util.Optional;

final class ConsumerRateInfo {
    private final String consumerId;
    private final Optional<MaxRate> maxRate;
    private final RateHistory history;

    ConsumerRateInfo(String consumerId, Optional<MaxRate> maxRate, RateHistory history) {
        this.consumerId = consumerId;
        this.maxRate = maxRate;
        this.history = history;
    }

    public Optional<MaxRate> getMaxRate() {
        return maxRate;
    }

    public RateHistory getHistory() {
        return history;
    }

    public String getConsumerId() {
        return consumerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsumerRateInfo that = (ConsumerRateInfo) o;
        return Objects.equals(consumerId, that.consumerId) &&
                Objects.equals(maxRate, that.maxRate) &&
                Objects.equals(history, that.history);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumerId, maxRate, history);
    }

    @Override
    public String toString() {
        return "ConsumerRateInfo{" +
                "consumerId='" + consumerId + '\'' +
                ", maxRate=" + maxRate +
                ", history=" + history +
                '}';
    }
}
