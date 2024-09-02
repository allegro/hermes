package pl.allegro.tech.hermes.schema.confluent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

class SchemaRegistryCompatibilityResponse {

  private final boolean compatible;

  @JsonCreator
  SchemaRegistryCompatibilityResponse(@JsonProperty("is_compatible") boolean compatible) {
    this.compatible = compatible;
  }

  public boolean isCompatible() {
    return compatible;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SchemaRegistryCompatibilityResponse that = (SchemaRegistryCompatibilityResponse) o;
    return compatible == that.compatible;
  }

  @Override
  public int hashCode() {
    return Objects.hash(compatible);
  }
}
