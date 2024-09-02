package pl.allegro.tech.hermes.schema.confluent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import pl.allegro.tech.hermes.api.RawSchema;

class SchemaRegistryRequestResponse {

  private final String schema;

  @JsonCreator
  SchemaRegistryRequestResponse(@JsonProperty("schema") String schema) {
    this.schema = schema;
  }

  static SchemaRegistryRequestResponse fromRawSchema(RawSchema rawSchema) {
    return new SchemaRegistryRequestResponse(rawSchema.value());
  }

  public String getSchema() {
    return schema;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SchemaRegistryRequestResponse that = (SchemaRegistryRequestResponse) o;
    return Objects.equals(schema, that.schema);
  }

  @Override
  public int hashCode() {
    return Objects.hash(schema);
  }
}
