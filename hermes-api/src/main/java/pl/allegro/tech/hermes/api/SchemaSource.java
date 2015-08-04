package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SchemaSource {

    private final String value;

    @JsonCreator
    private SchemaSource(@JsonProperty("schema") String value) {
        this.value = checkNotNull(value);
    }

    public static SchemaSource valueOf(String schemaSource) {
        return new SchemaSource(schemaSource);
    }

    @JsonProperty("schema")
    public String value() {
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

        SchemaSource that = (SchemaSource) o;

        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return "SchemaSource(" + value + ")";
    }
}
