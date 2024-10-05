package pl.allegro.tech.hermes.test.helper.avro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;

public interface RecordToBytesConverter {

  static byte[] recordToBytes(GenericRecord record, Schema schema) throws IOException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
      DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);

      writer.write(record, encoder);
      encoder.flush();
      return out.toByteArray();
    }
  }

  static GenericRecord bytesToRecord(byte[] data, Schema schema) throws IOException {
    GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
    BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(data, null);
    return reader.read(null, binaryDecoder);
  }
}
