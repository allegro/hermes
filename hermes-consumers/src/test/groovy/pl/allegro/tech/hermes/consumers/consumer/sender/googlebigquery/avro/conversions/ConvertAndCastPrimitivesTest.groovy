package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.conversions

import com.google.protobuf.DynamicMessage
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.junit.jupiter.api.Test
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.AvroTrait
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroToProtoConverter
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.DoubleProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.FloatProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.Int32Proto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.Int64Proto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.StringProto
import spock.lang.Specification

class ConvertAndCastPrimitivesTest  extends Specification  implements AvroTrait{

    @Test

    void 'should convert and cast primitive types'() {

        given:
        Schema schema = getSchemaFromResources("${suite}/${avroType}")
        String fieldName = "field"
        GenericRecord record = new GenericRecordBuilder(schema).set(fieldName, avroValue).build()
        GoogleBigQueryAvroToProtoConverter converter = new GoogleBigQueryAvroToProtoConverter()

        when:
        DynamicMessage message = converter.convertToProtoMessage(protoDescriptor, record)


        then:
        message != null
        println(message)
        def field = message.getDescriptorForType().findFieldByName(fieldName)
        def converted_value = message.getField(field)
        if (field.getDefaultValue() != avroValue) {
            record.hasField(fieldName) == message.hasField(field)
        }
        transformResult(converted_value) == expectedProtoValue


        where:
        suite        | avroType  | protoDescriptor                              | avroValue | expectedProtoValue | transformResult
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | "value" | "value" | { c -> c }
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | 12        | "12"               | { c -> c }
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | 12l       | "12"               | { c -> c }
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | 12f       | "12.0"             | { c -> c }
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | 12d       | "12.0"             | { c -> c }

        "primitives" | "int"     | Int32Proto.PrimitivesInt32.getDescriptor()   | 12      | 12      | { c -> c }
        "primitives" | "long"    | Int32Proto.PrimitivesInt32.getDescriptor()   | 12l       | 12                 | { c -> c }
        "primitives" | "float"   | Int32Proto.PrimitivesInt32.getDescriptor()   | 12f       | 12                 | { c -> c }
        "primitives" | "double"  | Int32Proto.PrimitivesInt32.getDescriptor()   | 12d       | 12                 | { c -> c }
        "primitives" | "string"  | Int32Proto.PrimitivesInt32.getDescriptor()   | "12"      | 12                 | { c -> c }
        "primitives" | "boolean" | Int32Proto.PrimitivesInt32.getDescriptor()   | true      | 1                  | { c -> c }
        "primitives" | "boolean" | Int32Proto.PrimitivesInt32.getDescriptor()   | false     | 0                  | { c -> c }

        "primitives" | "int"     | Int64Proto.PrimitivesInt64.getDescriptor()   | 12      | 12l     | { c -> c }
        "primitives" | "long"    | Int64Proto.PrimitivesInt64.getDescriptor()   | 12l       | 12l                | { c -> c }
        "primitives" | "float"   | Int64Proto.PrimitivesInt64.getDescriptor()   | 12f       | 12l                | { c -> c }
        "primitives" | "double"  | Int64Proto.PrimitivesInt64.getDescriptor()   | 12d       | 12l                | { c -> c }
        "primitives" | "string"  | Int64Proto.PrimitivesInt64.getDescriptor()   | "12.0"    | 12l                | { c -> c }
        "primitives" | "boolean" | Int64Proto.PrimitivesInt64.getDescriptor()   | true      | 1l                 | { c -> c }
        "primitives" | "boolean" | Int64Proto.PrimitivesInt64.getDescriptor()   | false     | 0l                 | { c -> c }

        "primitives" | "int"     | FloatProto.PrimitivesFloat.getDescriptor()   | 12      | 12.0f   | { c -> c }
        "primitives" | "long"    | FloatProto.PrimitivesFloat.getDescriptor()   | 12l       | 12.0f              | { c -> c }
        "primitives" | "float"   | FloatProto.PrimitivesFloat.getDescriptor()   | 12.234f   | 12.234f            | { c -> c }
        "primitives" | "double"  | FloatProto.PrimitivesFloat.getDescriptor()   | 12.234d   | 12.234f            | { c -> c }
        "primitives" | "string"  | FloatProto.PrimitivesFloat.getDescriptor()   | "12.234"  | 12.234f            | { c -> c }
        "primitives" | "boolean" | FloatProto.PrimitivesFloat.getDescriptor()   | true      | 1f                 | { c -> c }
        "primitives" | "boolean" | FloatProto.PrimitivesFloat.getDescriptor()   | false     | 0f                 | { c -> c }

        "primitives" | "int"     | DoubleProto.PrimitivesDouble.getDescriptor() | 12      | 12.0d   | { c -> c }
        "primitives" | "long"    | DoubleProto.PrimitivesDouble.getDescriptor() | 12l       | 12.0d              | { c -> c }
        "primitives" | "float"   | DoubleProto.PrimitivesDouble.getDescriptor() | 12.5f     | 12.5d              | { c -> c }
        "primitives" | "double"  | DoubleProto.PrimitivesDouble.getDescriptor() | 12.234d   | 12.234d            | { c -> c }
        "primitives" | "string"  | DoubleProto.PrimitivesDouble.getDescriptor() | "12.234"  | 12.234d            | { c -> c }
        "primitives" | "boolean" | DoubleProto.PrimitivesDouble.getDescriptor() | true      | 1d                 | { c -> c }
        "primitives" | "boolean" | DoubleProto.PrimitivesDouble.getDescriptor() | false     | 0d                 | { c -> c }

        "primitives" | "boolean" | BoolProto.PrimitivesBool.getDescriptor()     | true    | true    | { c -> c }
        "primitives" | "boolean" | BoolProto.PrimitivesBool.getDescriptor()     | false     | false              | { c -> c }
        "primitives" | "string"  | BoolProto.PrimitivesBool.getDescriptor()     | "true"    | true               | { c -> c }
        "primitives" | "string"  | BoolProto.PrimitivesBool.getDescriptor()     | "false"   | false              | { c -> c }
        "primitives" | "string"  | BoolProto.PrimitivesBool.getDescriptor()     | "True"    | true               | { c -> c }
        "primitives" | "string"  | BoolProto.PrimitivesBool.getDescriptor()     | "False"   | false              | { c -> c }
        "primitives" | "boolean" | BoolProto.PrimitivesBool.getDescriptor()     | 1         | true               | { c -> c }
        "primitives" | "boolean" | BoolProto.PrimitivesBool.getDescriptor()     | 0         | false              | { c -> c }
    }
}
