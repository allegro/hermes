package pl.allegro.tech.hermes.api;

import com.google.common.base.MoreObjects;

import java.util.Objects;

public final class SchemaData {

    private final RawSchema schema;

    private final int id;

    private final int version;

    private SchemaData(RawSchema schema, int id, int version) {
        this.schema = schema;
        this.id = id;
        this.version = version;
    }

    public static SchemaData valueOf(String schema, int id, int version) {
        return new SchemaData(RawSchema.valueOf(schema), id, version);
    }

    public RawSchema getSchema() { return schema; }

    public int getId() { return id; }

    public int getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SchemaData that = (SchemaData) o;

        return Objects.equals(id, that.id) && schema.equals(that.schema) && Objects.equals(version, that.version);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("schema", schema)
            .add("id", id)
            .add("version", version)
            .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, id, version);
    }
}
