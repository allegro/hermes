package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

import static java.lang.Double.parseDouble;

public class MetricDecimalValue {
    private static final String UNAVAILABLE_STRING = "unavailable";
    private static final MetricDecimalValue UNAVAILABLE = new MetricDecimalValue(true, "-1.0");

    private final boolean unavailable;
    private final String value;

    private MetricDecimalValue(boolean unavailable, String value) {
        this.unavailable = unavailable;
        this.value = value;
    }

    public static MetricDecimalValue unavailable() {
        return UNAVAILABLE;
    }

    public static MetricDecimalValue of(String value) {
        return new MetricDecimalValue(false, value);
    }

    @JsonCreator
    public static MetricDecimalValue deserialize(String value) {
        if (UNAVAILABLE_STRING.equals(value)) {
            return unavailable();
        }
        return of(value);
    }

    @JsonValue
    public String asString() {
        return unavailable ? UNAVAILABLE_STRING : value;
    }

    public double toDouble() {
        return parseDouble(value);
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
        MetricDecimalValue that = (MetricDecimalValue) o;
        return unavailable == that.unavailable &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unavailable, value);
    }
}
