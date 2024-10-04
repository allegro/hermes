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
}