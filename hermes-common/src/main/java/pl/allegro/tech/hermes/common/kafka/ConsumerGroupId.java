package pl.allegro.tech.hermes.common.kafka;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

public class ConsumerGroupId {

  private final String value;

  private ConsumerGroupId(String value) {
    this.value = checkNotNull(value);
  }

  public static ConsumerGroupId valueOf(String value) {
    return new ConsumerGroupId(value);
  }

  public String asString() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsumerGroupId that = (ConsumerGroupId) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "ConsumerGroupId(" + value + ")";
  }
}
