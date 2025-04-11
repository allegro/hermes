package pl.allegro.tech.hermes.api;

import static java.lang.Double.parseDouble;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public class MetricDecimalValue implements MetricValue {
  private static final String UNAVAILABLE_STRING = "unavailable";
  private static final MetricDecimalValue UNAVAILABLE = new MetricDecimalValue(false, "-1.0");
  private static final MetricDecimalValue DEFAULT_VALUE = new MetricDecimalValue(true, "0.0");

  private final boolean available;
  private final String value;

  private MetricDecimalValue(boolean available, String value) {
    this.available = available;
    this.value = value;
  }

  public static MetricDecimalValue unavailable() {
    return UNAVAILABLE;
  }

  public static MetricDecimalValue defaultValue() {
    return DEFAULT_VALUE;
  }

  public static MetricDecimalValue of(String value) {
    return new MetricDecimalValue(true, value);
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
    return available ? value : UNAVAILABLE_STRING;
  }

  public double toDouble() {
    return parseDouble(value);
  }

  public boolean isAvailable() {
    return available;
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
    return available == that.available && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(available, value);
  }

  @Override
  public String toString() {
    return "MetricDecimalValue{" + "available=" + available + ", value='" + value + '\'' + '}';
  }
}
