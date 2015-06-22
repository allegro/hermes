package pl.allegro.tech.hermes.test.helper.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.io.IOException;

import static pl.allegro.tech.hermes.test.helper.avro.RecordToBytesConverter.recordToBytes;

public class AvroUser {

    private final Schema schema;

    public AvroUser() throws IOException {
        this.schema = new Schema.Parser().parse(this.getClass().getResourceAsStream("/schema/user.avsc"));
    }

    public Schema getSchema() {
        return schema;
    }

    public byte [] create(String name, int age, String favoriteColor) throws IOException {
        GenericRecord user = new GenericData.Record(schema);
        user.put("name", name);
        user.put("age", age);
        user.put("favoriteColor", favoriteColor);

        return userToBytes(user);
    }

    public byte[] userToBytes(GenericRecord user) throws IOException {
        return recordToBytes(user, schema);
    }
}
