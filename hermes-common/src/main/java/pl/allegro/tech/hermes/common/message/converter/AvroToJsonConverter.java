package pl.allegro.tech.hermes.common.message.converter;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroToJsonConverter {

    private final GenericDatumReader<GenericRecord> reader;
    private final GenericDatumWriter<GenericRecord> writer;
    private final Schema schema;

    public AvroToJsonConverter(Schema schema) {
        this.schema = schema;
        this.reader = new GenericDatumReader<>(schema);
        this.writer = new GenericDatumWriter<>(schema);
    }

    public byte [] convert(byte [] data) {
        try {
            BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(data, null);
            GenericRecord record = reader.read(null, binaryDecoder);

            return decodeRecordToJson(record);
        } catch (IOException exception) {
            throw new ConvertingException("Could not convert avro message to json. Message invalid with schema.", exception);
        }
    }

    private byte [] decodeRecordToJson(GenericRecord record) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(schema, outputStream);
        writer.write(record, jsonEncoder);
        jsonEncoder.flush();
        outputStream.close();

        return outputStream.toByteArray();
    }

}
