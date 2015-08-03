package pl.allegro.tech.hermes.common.message.converter;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonDecoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class JsonToAvroConverter {

    public byte[] convert(byte[] data, Schema schema) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            GenericDatumWriter<Object> writer = new GenericDatumWriter<>(schema);
            writer.write(readRecord(data, schema), encoder);
            encoder.flush();
            return outputStream.toByteArray();
        } catch (AvroRuntimeException | IOException e) {
            throw new ConvertingException("Could not convert JSON to AVRO.", e);
        }
    }

    private GenericData.Record readRecord(byte[] data, Schema schema) throws IOException {
        JsonDecoder decoder = DecoderFactory.get().jsonDecoder(schema, new ByteArrayInputStream(data));
        DatumReader<GenericData.Record> reader = new GenericDatumReader<>(schema);
        return reader.read(null, decoder);
    }
}
