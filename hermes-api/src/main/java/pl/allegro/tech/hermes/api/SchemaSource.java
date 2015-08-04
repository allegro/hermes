package pl.allegro.tech.hermes.api;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

public final class SchemaSource {

    private final String value;

    private SchemaSource(String value) {
        this.value = checkNotNull(emptyToNull(value));
    }

    public static SchemaSource valueOf(String schemaSource) {
        return new SchemaSource(schemaSource);
    }

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
