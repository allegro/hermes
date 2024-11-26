package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.conversions

import com.google.protobuf.DynamicMessage
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.junit.jupiter.api.Test
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.AvroTrait
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroToProtoConverter
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.RecordBoolProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.RecordBytesProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.RecordDoubleProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.RecordFloatProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.RecordInt32Proto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.RecordInt64Proto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.RecordStringProto
import spock.lang.Specification

import java.nio.ByteBuffer

class ConvertRecordWithPrimitivesTest  extends Specification implements AvroTrait {
    @Test
    void 'should convert record with primitive types'() {

        given:
        Schema schema = getSchemaFromResources("${suite}/${avroType}")
        String fieldName = "field"
        String valueFieldName = "value_field"
        GenericRecord record = new GenericRecordBuilder(schema).set(
                fieldName,
                new GenericRecordBuilder(schema.getField(fieldName).schema()).set(valueFieldName, avroValue).build()
        ).build()
        GoogleBigQueryAvroToProtoConverter converter = new GoogleBigQueryAvroToProtoConverter()

        when:
        println("descriptor.fields = ${protoDescriptor.findFieldByName("field").messageType.nestedTypes}")

        DynamicMessage message = converter.convertToProtoMessage(protoDescriptor, record)


        then:
        message != null
        println(message)
        def field = message.getDescriptorForType().findFieldByName(fieldName)
        def convertedFieldValue = message.getField(field).toString().replace("\\", "").replace("\n", "")
        def expectedRecord = "value_field: ${expectedProtoValue}".replace("\\", "").replace("\n", "")
        expectedRecord == convertedFieldValue


        where:
        suite               | avroType  | protoDescriptor                                          | avroValue                         | expectedProtoValue | transformResult
        "record-primitives" | "string"  | RecordStringProto.RecordPrimitivesString.getDescriptor() | "value"                           | "\"value\""        | { c -> c }
//        "record-primitives" | "int"     | RecordInt32Proto.RecordPrimitivesInt32.getDescriptor()   | 12                                | 12                 | { c -> c }
//        "record-primitives" | "long"    | RecordInt64Proto.RecordPrimitivesInt64.getDescriptor()   | 12l                               | 12l                | { c -> c }
//        "record-primitives" | "boolean" | RecordInt64Proto.RecordPrimitivesInt64.getDescriptor()   | 12l                               | 12l                | { c -> c }
//        "record-primitives" | "boolean" | RecordBoolProto.RecordPrimitivesBool.getDescriptor()     | true                              | true               | { c -> c }
//        "record-primitives" | "bytes"   | RecordBytesProto.RecordPrimitivesBytes.getDescriptor()   | ByteBuffer.wrap("123".getBytes()) | "\"123\""          | { c -> new String(c.bytes) }
//        "record-primitives" | "float"   | RecordFloatProto.RecordPrimitivesFloat.getDescriptor()   | 1.234f                            | 1.234f             | { c -> c }
//        "record-primitives" | "double"  | RecordDoubleProto.RecordPrimitivesDouble.getDescriptor() | 1.234d                            | 1.234d             | { c -> c }


    }

}
