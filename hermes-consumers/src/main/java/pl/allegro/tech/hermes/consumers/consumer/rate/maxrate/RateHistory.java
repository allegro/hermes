package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.google.common.base.Preconditions;

import java.beans.ConstructorProperties;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class RateHistory {
    private final List<Double> rates;

    @ConstructorProperties({"rates"})
    public RateHistory(List<Double> rates) {
        this.rates = rates;
    }

    public List<Double> getRates() {
        return rates;
    }

    static RateHistory updatedRates(RateHistory history, double newRate, int limit) {
        Preconditions.checkArgument(limit > 0);
        List<Double> rates = Stream.concat(
                Stream.of(newRate), history.getRates().stream().limit(limit - 1)).collect(Collectors.toList());
        return new RateHistory(rates);
    }

    static RateHistory create(double rate) {
        return new RateHistory(Arrays.asList(rate));
    }

    static RateHistory empty() {
        return new RateHistory(Collections.emptyList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RateHistory that = (RateHistory) o;
        return Objects.equals(rates, that.rates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rates);
    }

    @Override
    public String toString() {
        return "RateHistory{" +
                "rates=" + rates +
                '}';
    }
}
