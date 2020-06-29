package pl.allegro.tech.hermes.api;

import com.google.common.base.MoreObjects;

import java.util.Objects;

public final class SchemaWithId {

    private final RawSchema schema;

    private final int id;

    private SchemaWithId(RawSchema schema, int id) {
        this.schema = schema;
        this.id = id;
    }

    public static SchemaWithId valueOf(String schema, int id) {
        return new SchemaWithId(RawSchema.valueOf(schema), id);
    }

    public RawSchema getSchema() { return schema; }

    public int getId() { return id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SchemaWithId that = (SchemaWithId) o;

        return Objects.equals(id, that.id) && schema.equals(that.schema);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("schema", schema)
            .add("id", id)
            .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, id);
    }
}
