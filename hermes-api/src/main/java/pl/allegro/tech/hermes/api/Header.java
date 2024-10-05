package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

public class Header {

  @NotNull private String name;

  @NotNull private String value;

  @JsonCreator
  public Header(@JsonProperty("name") String name, @JsonProperty("value") String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
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
    Header header = (Header) o;
    return Objects.equals(name, header.name) && Objects.equals(value, header.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }
}
