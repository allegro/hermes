package pl.allegro.tech.hermes.test.helper.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.io.IOException;

import static pl.allegro.tech.hermes.test.helper.avro.RecordToBytesConverter.recordToBytes;

public class AvroUser {

    private static final String NAME_FIELD = "name";
    private static final String AGE_FIELD = "age";
    private static final String FAVORITE_COLOR_FIELD = "favoriteColor";

    private final Schema schema;

    public AvroUser() throws IOException {
        this.schema = new Schema.Parser().parse(this.getClass().getResourceAsStream("/schema/user.avsc"));
    }

    public Schema getSchema() {
        return schema;
    }

    public byte[] create(String name, int age, String favoriteColor) throws IOException {
        GenericRecord user = new GenericData.Record(schema);
        user.put(NAME_FIELD, name);
        user.put(AGE_FIELD, age);
        user.put(FAVORITE_COLOR_FIELD, favoriteColor);

        return userToBytes(user);
    }

    public byte[] userToBytes(GenericRecord user) throws IOException {
        return recordToBytes(user, schema);
    }

    public TestMessage createMessage(String name, int age, String color) throws IOException {
        return TestMessage.of(NAME_FIELD, name).append(AGE_FIELD, age).append(FAVORITE_COLOR_FIELD, color);
    }
}
