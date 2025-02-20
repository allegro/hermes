package pl.allegro.tech.hermes.schema;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import pl.allegro.tech.hermes.api.RawSchemaWithMetadata;

public class CompiledSchema<T> {

  private final T schema;
  private final SchemaId id;
  private final SchemaVersion version;

  public CompiledSchema(T schema, SchemaId id, SchemaVersion version) {
    this.schema = schema;
    this.id = id;
    this.version = version;
  }

  public static <T> CompiledSchema<T> of(T schema, int id, int version) {
    return new CompiledSchema<>(schema, SchemaId.valueOf(id), SchemaVersion.valueOf(version));
  }

  public static <T> CompiledSchema<T> of(
      SchemaCompiler<T> schemaCompiler, RawSchemaWithMetadata rawSchemaWithMetadata) {
    return CompiledSchema.of(
        schemaCompiler.compile(rawSchemaWithMetadata.getSchema()),
        rawSchemaWithMetadata.getId(),
        rawSchemaWithMetadata.getVersion());
  }

  public T getSchema() {
    return schema;
  }

  public SchemaVersion getVersion() {
    return version;
  }

  public SchemaId getId() {
    return id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(schema, id, version);
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
    return Objects.equals(id, that.id)
        && Objects.equals(version, that.version)
        && Objects.equals(schema, that.schema);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("schema", schema)
        .add("id", id)
        .add("version", version)
        .toString();
  }
}
