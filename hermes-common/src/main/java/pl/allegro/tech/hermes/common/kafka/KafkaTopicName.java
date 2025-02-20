package pl.allegro.tech.hermes.common.kafka;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class KafkaTopicName {

  private final String value;

  private KafkaTopicName(String value) {
    this.value = checkNotNull(value);
  }

  @JsonCreator
  public static KafkaTopicName valueOf(@JsonProperty("value") String value) {
    return new KafkaTopicName(value);
  }

  @JsonGetter(value = "value")
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
    KafkaTopicName that = (KafkaTopicName) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "KafkaTopicName(" + value + ")";
  }
}
