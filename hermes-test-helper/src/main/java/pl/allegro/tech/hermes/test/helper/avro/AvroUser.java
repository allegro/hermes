package pl.allegro.tech.hermes.test.helper.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.io.IOException;
import java.io.UncheckedIOException;

import static pl.allegro.tech.hermes.test.helper.avro.RecordToBytesConverter.recordToBytes;

public class AvroUser {

    private static final String NAME_FIELD = "name";
    private static final String AGE_FIELD = "age";
    private static final String FAVORITE_COLOR_FIELD = "favoriteColor";

    private final Schema schema;
    private final GenericRecord data;

    public AvroUser() {
        this(AvroUserSchemaLoader.load(), "", 0, "") ;
    }

    public AvroUser(String name, int age, String favouriteColour) {
        this(AvroUserSchemaLoader.load(), name, age, favouriteColour);
    }

    public AvroUser(Schema schema, String name, int age, String favouriteColour) {
        this.schema = schema;
        this.data = create(name, age, favouriteColour);
    }

    public AvroUser(Schema schema, byte[] bytes) throws IOException {
        this.schema = schema;
        this.data = RecordToBytesConverter.bytesToRecord(bytes, schema);
    }

    public Schema getSchema() {
        return schema;
    }

    public String getSchemaAsString() {
        return schema.toString();
    }

    public String getName() {
        return data.get(NAME_FIELD).toString();
    }

    public int getAge() {
        return Integer.valueOf(data.get(AGE_FIELD).toString());
    }

    public String getFavoriteColor() {
        return data.get(FAVORITE_COLOR_FIELD).toString();
    }

    public byte[] asBytes() throws IOException {
        return recordToBytes(data, schema);
    }

    public TestMessage asTestMessage() {
        return TestMessage.of(NAME_FIELD, getName()).append(AGE_FIELD, getAge()).append(FAVORITE_COLOR_FIELD, getFavoriteColor());
    }

    public static AvroUser create(Schema schema, byte[] bytes) {
        try {
            return new AvroUser(schema, bytes);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private GenericRecord create(String name, int age, String favoriteColor) {
        GenericRecord user = new GenericData.Record(schema);
        user.put(NAME_FIELD, name);
        user.put(AGE_FIELD, age);
        user.put(FAVORITE_COLOR_FIELD, favoriteColor);

        return user;
    }
}
