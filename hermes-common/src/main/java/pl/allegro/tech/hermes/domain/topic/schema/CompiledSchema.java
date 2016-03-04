package pl.allegro.tech.hermes.domain.topic.schema;

import java.util.Objects;

public class CompiledSchema<T> {

    private final T schema;
    private final SchemaVersion version;

    public CompiledSchema(T schema, SchemaVersion version) {
        this.schema = schema;
        this.version = version;
    }

    public T getSchema() {
        return schema;
    }

    public SchemaVersion getVersion() {
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

        CompiledSchema<?> that = (CompiledSchema<?>) o;
        return Objects.equals(version, that.version) && Objects.equals(schema, that.schema);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("schema", schema)
                .add("version", version)
                .toString();
    }
}
