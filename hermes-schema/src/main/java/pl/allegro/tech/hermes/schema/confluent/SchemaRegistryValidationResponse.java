package pl.allegro.tech.hermes.schema.confluent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.stream.Collectors;

class SchemaRegistryValidationResponse {

  private final boolean valid;
  private final List<SchemaRegistryValidationError> errors;

  @JsonCreator
  SchemaRegistryValidationResponse(
      @JsonProperty("is_valid") boolean valid,
      @JsonProperty("errors") List<SchemaRegistryValidationError> errors) {
    this.valid = valid;
    this.errors = errors;
  }

  public boolean isValid() {
    return valid;
  }

  @JsonIgnore
  public String getErrorsMessage() {
    return errors.stream()
        .map(SchemaRegistryValidationError::getMessage)
        .collect(Collectors.joining(". "));
  }
}
