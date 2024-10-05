package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public class MetricLongValue {
  private static final String UNAVAILABLE_STRING = "unavailable";
  private static final MetricLongValue UNAVAILABLE = new MetricLongValue(false, -1);

  private final boolean available;
  private final long value;

  private MetricLongValue(boolean available, long value) {
    this.available = available;
    this.value = value;
  }

  public static MetricLongValue unavailable() {
    return UNAVAILABLE;
  }

  public static MetricLongValue of(long value) {
    return new MetricLongValue(true, value);
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
    return available ? String.valueOf(value) : UNAVAILABLE_STRING;
  }

  public long toLong() {
    return value;
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
    MetricLongValue that = (MetricLongValue) o;
    return available == that.available && value == that.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(available, value);
  }
}
