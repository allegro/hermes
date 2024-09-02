package pl.allegro.tech.hermes.common.message.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;

public interface AvroRecordToBytesConverter {

  static GenericRecord bytesToRecord(byte[] data, Schema schema) {
    return AvroBinaryDecoders.decodeReusingThreadLocalBinaryDecoder(data, schema);
  }

  static byte[] recordToBytes(GenericRecord genericRecord, Schema schema) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    BinaryEncoder binaryEncoder = EncoderFactory.get().binaryEncoder(outputStream, null);
    new GenericDatumWriter<>(schema).write(genericRecord, binaryEncoder);
    binaryEncoder.flush();

    return outputStream.toByteArray();
  }
}
