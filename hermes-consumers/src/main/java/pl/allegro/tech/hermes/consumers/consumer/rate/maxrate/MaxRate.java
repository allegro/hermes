package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.beans.ConstructorProperties;
import java.util.Objects;

final class MaxRate {
    private final double maxRate;

    @ConstructorProperties({"maxRate"})
    MaxRate(double maxRate) {
        this.maxRate = maxRate;
    }

    public double getMaxRate() {
        return maxRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaxRate maxRate1 = (MaxRate) o;
        return Double.compare(maxRate1.maxRate, maxRate) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxRate);
    }

    @Override
    public String toString() {
        return "MaxRate{" +
                "maxRate=" + maxRate +
                '}';
    }
}
