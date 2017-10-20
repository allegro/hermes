package pl.allegro.tech.hermes.frontend.publishing.message;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.*;
import tech.allegro.schema.json2avro.converter.AvroConversionException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

class AvroEncodedJsonAvroConverter {

    byte[] convertToAvro(byte[] bytes, Schema schema) {
        try {
            return convertToAvro(readJson(bytes, schema), schema);
        } catch (IOException e) {
            throw new AvroConversionException("Failed to convert to AVRO.", e);
        } catch (AvroRuntimeException e) {
                throw new AvroConversionException(
                        String.format("Failed to convert to AVRO: %s.", e.getMessage()), e);
        }
    }

    private GenericData.Record readJson(byte[] bytes, Schema schema) throws IOException {
        InputStream input = new ByteArrayInputStream(bytes);
        Decoder decoder = DecoderFactory.get().jsonDecoder(schema, input);
        return new GenericDatumReader<GenericData.Record>(schema).read(null, decoder);
    }

    private byte[] convertToAvro(GenericData.Record jsonData, Schema schema) throws IOException {
        GenericDatumWriter<GenericData.Record> writer = new GenericDatumWriter<>(schema);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        writer.write(jsonData, encoder);
        encoder.flush();
        return outputStream.toByteArray();
    }
}