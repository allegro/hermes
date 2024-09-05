package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro

import org.apache.avro.AvroRuntimeException
import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DatumReader
import org.apache.avro.io.DecoderFactory

trait AvroTrait {

    Schema getSchemaFromResources(String fileName) {
        return new Schema.Parser().parse(AvroTrait.class.getClassLoader().getResourceAsStream("schemas/${fileName}.avsc"))
    }

    GenericRecord getGenericRecordFromFile(String payloadFile, Schema schema) {
        def uri = this.class.getClassLoader().getResource(payloadFile).toURI()
        def avroFile = new File(uri)
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema, schema)

        for (int i = 0; i <= 5; i++) { // don't ask
            try {
                byte[] bs = Arrays.copyOfRange(avroFile.bytes, i, avroFile.bytes.length)
                def binaryDecoder = DecoderFactory.get().binaryDecoder(bs, null)

                return reader.read(null, binaryDecoder)
            } catch (ArrayIndexOutOfBoundsException|AvroRuntimeException|IOException ignored) {
            }
        }

        throw new RuntimeException("Cannot decode")
    }
}