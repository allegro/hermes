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

public interface AvroToJsonConverter {

    static byte[] convert(byte[] data, Schema schema) {
        try {
            BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(data, null);
            GenericRecord record = new GenericDatumReader<GenericRecord>(schema).read(null, binaryDecoder);

            return decodeRecordToJson(record, schema);
        } catch (IOException exception) {
            throw new ConvertingException("Could not convert avro message to json. Invalid Message.", exception);
        }
    }

    static byte[] decodeRecordToJson(GenericRecord record, Schema schema) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(schema, outputStream);
        new GenericDatumWriter<>(schema).write(record, jsonEncoder);
        jsonEncoder.flush();

        return outputStream.toByteArray();
    }

}
