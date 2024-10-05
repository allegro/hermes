package pl.allegro.tech.hermes.api;

import com.google.common.base.MoreObjects;
import java.util.Objects;

public final class RawSchemaWithMetadata {

  private final RawSchema schema;
  private final int id;
  private final int version;

  private RawSchemaWithMetadata(RawSchema schema, int id, int version) {
    this.schema = schema;
    this.id = id;
    this.version = version;
  }

  public static RawSchemaWithMetadata of(String schema, int id, int version) {
    return new RawSchemaWithMetadata(RawSchema.valueOf(schema), id, version);
  }

  public RawSchema getSchema() {
    return schema;
  }

  public String getSchemaString() {
    return schema.value();
  }

  public int getId() {
    return id;
  }

  public int getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RawSchemaWithMetadata that = (RawSchemaWithMetadata) o;

    return schema.equals(that.schema)
        && Objects.equals(id, that.id)
        && Objects.equals(version, that.version);
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
