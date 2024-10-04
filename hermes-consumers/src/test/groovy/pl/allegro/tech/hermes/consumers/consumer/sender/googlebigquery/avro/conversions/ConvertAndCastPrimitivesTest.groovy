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

class ConvertAndCastPrimitivesTest extends Specification implements AvroTrait {

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
        converted_value == expectedProtoValue


        where:
        suite        | avroType  | protoDescriptor                              | avroValue | expectedProtoValue
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | "value"   | "value"
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | 12        | "12"
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | 12l       | "12"
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | 12f       | "12.0"
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | 12d       | "12.0"

        "primitives" | "int"     | Int32Proto.PrimitivesInt32.getDescriptor()   | 12        | 12
        "primitives" | "long"    | Int32Proto.PrimitivesInt32.getDescriptor()   | 12l       | 12
        "primitives" | "float"   | Int32Proto.PrimitivesInt32.getDescriptor()   | 12f       | 12
        "primitives" | "double"  | Int32Proto.PrimitivesInt32.getDescriptor()   | 12d       | 12
        "primitives" | "string"  | Int32Proto.PrimitivesInt32.getDescriptor()   | "12"      | 12
        "primitives" | "boolean" | Int32Proto.PrimitivesInt32.getDescriptor()   | true      | 1
        "primitives" | "boolean" | Int32Proto.PrimitivesInt32.getDescriptor()   | false     | 0

        "primitives" | "int"     | Int64Proto.PrimitivesInt64.getDescriptor()   | 12        | 12l
        "primitives" | "long"    | Int64Proto.PrimitivesInt64.getDescriptor()   | 12l       | 12l
        "primitives" | "float"   | Int64Proto.PrimitivesInt64.getDescriptor()   | 12f       | 12l
        "primitives" | "double"  | Int64Proto.PrimitivesInt64.getDescriptor()   | 12d       | 12l
        "primitives" | "string"  | Int64Proto.PrimitivesInt64.getDescriptor()   | "12.0"    | 12l
        "primitives" | "boolean" | Int64Proto.PrimitivesInt64.getDescriptor()   | true      | 1l
        "primitives" | "boolean" | Int64Proto.PrimitivesInt64.getDescriptor()   | false     | 0l

        "primitives" | "int"     | FloatProto.PrimitivesFloat.getDescriptor()   | 12        | 12.0f
        "primitives" | "long"    | FloatProto.PrimitivesFloat.getDescriptor()   | 12l       | 12.0f
        "primitives" | "float"   | FloatProto.PrimitivesFloat.getDescriptor()   | 12.234f   | 12.234f
        "primitives" | "double"  | FloatProto.PrimitivesFloat.getDescriptor()   | 12.234d   | 12.234f
        "primitives" | "string"  | FloatProto.PrimitivesFloat.getDescriptor()   | "12.234"  | 12.234f
        "primitives" | "boolean" | FloatProto.PrimitivesFloat.getDescriptor()   | true      | 1f
        "primitives" | "boolean" | FloatProto.PrimitivesFloat.getDescriptor()   | false     | 0f

        "primitives" | "int"     | DoubleProto.PrimitivesDouble.getDescriptor() | 12        | 12.0d
        "primitives" | "long"    | DoubleProto.PrimitivesDouble.getDescriptor() | 12l       | 12.0d
        "primitives" | "float"   | DoubleProto.PrimitivesDouble.getDescriptor() | 12.5f     | 12.5d
        "primitives" | "double"  | DoubleProto.PrimitivesDouble.getDescriptor() | 12.234d   | 12.234d
        "primitives" | "string"  | DoubleProto.PrimitivesDouble.getDescriptor() | "12.234"  | 12.234d
        "primitives" | "boolean" | DoubleProto.PrimitivesDouble.getDescriptor() | true      | 1d
        "primitives" | "boolean" | DoubleProto.PrimitivesDouble.getDescriptor() | false     | 0d

        "primitives" | "boolean" | BoolProto.PrimitivesBool.getDescriptor()     | true      | true
        "primitives" | "boolean" | BoolProto.PrimitivesBool.getDescriptor()     | false     | false
        "primitives" | "string"  | BoolProto.PrimitivesBool.getDescriptor()     | "true"    | true
        "primitives" | "string"  | BoolProto.PrimitivesBool.getDescriptor()     | "false"   | false
        "primitives" | "string"  | BoolProto.PrimitivesBool.getDescriptor()     | "True"    | true
        "primitives" | "string"  | BoolProto.PrimitivesBool.getDescriptor()     | "False"   | false
        "primitives" | "boolean" | BoolProto.PrimitivesBool.getDescriptor()     | 1         | true
        "primitives" | "boolean" | BoolProto.PrimitivesBool.getDescriptor()     | 0         | false
    }
}
