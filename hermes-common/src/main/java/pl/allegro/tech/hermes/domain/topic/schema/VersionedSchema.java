package pl.allegro.tech.hermes.domain.topic.schema;

import java.util.Objects;

public class VersionedSchema<T> {

    private final T schema;
    private final int version;

    public VersionedSchema(T schema, int version) {
        this.schema = schema;
        this.version = version;
    }

    public T getSchema() {
        return schema;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VersionedSchema<?> that = (VersionedSchema<?>) o;
        return version == that.version && Objects.equals(schema, that.schema);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("schema", schema)
                .add("version", version)
                .toString();
    }
}
