package pl.allegro.tech.hermes.schema;

import com.google.common.base.Joiner;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    public static String toString(List<SchemaVersion> versions) {
        return "[" + Joiner.on(',').join(versions.stream().map(SchemaVersion::value).collect(Collectors.toList())) + "]";
    }

}
