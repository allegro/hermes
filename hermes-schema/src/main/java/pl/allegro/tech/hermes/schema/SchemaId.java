package pl.allegro.tech.hermes.schema;

import java.util.Objects;

public final class SchemaId {

  private final int value;

  private SchemaId(int value) {
    this.value = value;
  }

  public static SchemaId valueOf(int id) {
    return new SchemaId(id);
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
    SchemaId that = (SchemaId) o;
    return value == that.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "SchemaId(" + value + ")";
  }
}
