package pl.allegro.tech.hermes.schema.confluent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import pl.allegro.tech.hermes.api.RawSchema;

import java.util.Objects;

class SchemaRegistryRequest {

    private final String schema;

    @JsonCreator
    SchemaRegistryRequest(@JsonProperty("schema") String schema) {
        this.schema = schema;
    }

    static SchemaRegistryRequest fromRawSchema(RawSchema rawSchema) {
        return new SchemaRegistryRequest(rawSchema.value());
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
        SchemaRegistryRequest that = (SchemaRegistryRequest) o;
        return Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema);
    }
}
