package pl.allegro.tech.hermes.test.helper.avro;

import static pl.allegro.tech.hermes.test.helper.avro.RecordToBytesConverter.recordToBytes;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class AvroUser {

  private static final String METADATA_FIELD = "__metadata";
  private static final String NAME_FIELD = "name";
  private static final String AGE_FIELD = "age";
  private static final String FAVORITE_COLOR_FIELD = "favoriteColor";

  private final CompiledSchema<Schema> schema;

  private final GenericRecord record;

  public AvroUser() {
    this("defaultName", 0, "defaultColor");
  }

  public AvroUser(CompiledSchema<Schema> schema) {
    this(schema, "defaultName", 0, "defaultColor");
  }

  public AvroUser(String name, int age, String favouriteColour) {
    this(CompiledSchema.of(AvroUserSchemaLoader.load(), 1, 1), name, age, favouriteColour);
  }

  public AvroUser(CompiledSchema<Schema> schema, String name, int age, String favouriteColour) {
    this.schema = schema;
    this.record = create(name, age, favouriteColour);
  }

  public AvroUser(CompiledSchema<Schema> schema, byte[] bytes) throws IOException {
    this.schema = schema;
    this.record = RecordToBytesConverter.bytesToRecord(bytes, schema.getSchema());
  }

  public Schema getSchema() {
    return schema.getSchema();
  }

  public String getSchemaAsString() {
    return schema.getSchema().toString();
  }

  public String getName() {
    return record.get(NAME_FIELD).toString();
  }

  public int getAge() {
    return Integer.parseInt(record.get(AGE_FIELD).toString());
  }

  public String getFavoriteColor() {
    return record.get(FAVORITE_COLOR_FIELD).toString();
  }

  public byte[] asBytes() {
    try {
      return recordToBytes(record, schema.getSchema());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String asJson() {
    return asTestMessage().toString();
  }

  public String asAvroEncodedJson() {
    return asAvroEncodedTestMessage().toString();
  }

  public TestMessage asTestMessage() {
    return TestMessage.of(NAME_FIELD, getName())
        .append(AGE_FIELD, getAge())
        .append(FAVORITE_COLOR_FIELD, getFavoriteColor());
  }

  public TestMessage asAvroEncodedTestMessage() {
    Object favoriteColorType =
        Optional.ofNullable(getFavoriteColor())
            .map(color -> (Object) ImmutableMap.of("string", color))
            .orElse("null");
    return TestMessage.of(METADATA_FIELD, null)
        .append(NAME_FIELD, getName())
        .append(AGE_FIELD, getAge())
        .append(FAVORITE_COLOR_FIELD, favoriteColorType);
  }

  public static AvroUser create(CompiledSchema<Schema> schema, byte[] bytes) {
    try {
      return new AvroUser(schema, bytes);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private GenericRecord create(String name, int age, String favoriteColor) {
    GenericRecord user = new GenericData.Record(schema.getSchema());
    user.put(NAME_FIELD, name);
    user.put(AGE_FIELD, age);
    user.put(FAVORITE_COLOR_FIELD, favoriteColor);

    return user;
  }

  public CompiledSchema<Schema> getCompiledSchema() {
    return schema;
  }
}
