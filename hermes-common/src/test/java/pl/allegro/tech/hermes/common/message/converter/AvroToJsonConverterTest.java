package pl.allegro.tech.hermes.common.message.converter;


import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.common.message.converter.AvroToJsonConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class AvroToJsonConverterTest {

    private AvroToJsonConverter avroToJsonConverter;
    private Schema schema;

    @Before
    public void setup() throws IOException {
        schema = new Schema.Parser().parse(getClass().getResourceAsStream("/schema/user.avsc"));
        avroToJsonConverter = new AvroToJsonConverter(schema);
    }

    @Test
    public void shouldConvertAvroToJson() throws IOException {
        //when
        byte [] json = avroToJsonConverter.convert(getSampleUser("Bob", 50, "blue"));

        //then
        assertThat(new String(json)).isEqualTo("{\"name\":\"Bob\",\"age\":50,\"favoriteColor\":\"blue\"}");
    }

    private byte [] getSampleUser(String name, int age, String favoriteColor) throws IOException {
        GenericRecord user = new GenericData.Record(schema);
        user.put("name", name);
        user.put("age", age);
        user.put("favoriteColor", favoriteColor);

        return genericRecordToBytes(user);
    }

    private byte[] genericRecordToBytes(GenericRecord genericRecord) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);

        writer.write(genericRecord, encoder);
        encoder.flush();
        out.close();
        return out.toByteArray();
    }
}