package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public class MetricLongValue {
    private static final String UNAVAILABLE_STRING = "unavailable";
    private static final MetricLongValue UNAVAILABLE = new MetricLongValue(true, -1);

    private final boolean unavailable;
    private final long value;

    private MetricLongValue(boolean unavailable, long value) {
        this.unavailable = unavailable;
        this.value = value;
    }

    public static MetricLongValue unavailable() {
        return UNAVAILABLE;
    }

    public static MetricLongValue of(long lag) {
        return new MetricLongValue(false, lag);
    }

    @JsonCreator
    public static MetricLongValue deserialize(String value) {
        if (UNAVAILABLE_STRING.equals(value)) {
            return unavailable();
        }
        return of(Long.valueOf(value));
    }

    @JsonValue
    public String asString() {
        return unavailable ? UNAVAILABLE_STRING : String.valueOf(value);
    }

    public long toLong() {
        return value;
    }

    public boolean isAvailable() {
        return !unavailable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetricLongValue that = (MetricLongValue) o;
        return unavailable == that.unavailable &&
                value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(unavailable, value);
    }
}
