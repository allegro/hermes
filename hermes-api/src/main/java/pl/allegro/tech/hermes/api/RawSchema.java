package pl.allegro.tech.hermes.api;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

public final class RawSchema {

    private final String value;

    private RawSchema(String value) {
        this.value = checkNotNull(emptyToNull(value));
    }

    public static RawSchema valueOf(String schema) {
        return new RawSchema(schema);
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

        RawSchema that = (RawSchema) o;

        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return "RawSource(" + value + ")";
    }
}
