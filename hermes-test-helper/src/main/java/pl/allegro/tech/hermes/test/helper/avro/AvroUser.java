package pl.allegro.tech.hermes.test.helper.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);

        writer.write(user, encoder);
        encoder.flush();
        return out.toByteArray();
    }
}
