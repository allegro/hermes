package pl.allegro.tech.hermes.domain.topic.schema;

import java.util.Objects;

public final class SchemaVersion {

    private final int value;

    private SchemaVersion(int value) {
        this.value = value;
    }

    public static SchemaVersion valueOf(int version) {
        return new SchemaVersion(version);
    }

    public int value() {
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
        SchemaVersion that = (SchemaVersion) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "SchemaVersion(" + value + ")";
    }
}
