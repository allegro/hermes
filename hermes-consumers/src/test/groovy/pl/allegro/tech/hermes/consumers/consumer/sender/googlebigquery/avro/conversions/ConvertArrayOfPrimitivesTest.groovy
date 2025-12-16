package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.conversions

import com.google.protobuf.DynamicMessage
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.junit.jupiter.api.Test
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.AvroTrait
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroToProtoConverter
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.ArrayBoolProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.ArrayBytesProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.ArrayDoubleProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.ArrayFloatProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.ArrayInt32Proto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.ArrayInt64Proto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.ArrayStringProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.StringToBoolProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.StringToBytesProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.StringToDoubleProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.StringToFloatProto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.StringToInt32Proto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.StringToInt64Proto
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.StringToStringProto
import spock.lang.Specification

import java.nio.ByteBuffer

class ConvertArrayOfPrimitivesTest extends Specification implements AvroTrait {

    @Test
    void 'should convert array primitive types'() {

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
        def converted_value = message.getField(field).collect { transformResult(it) }
        converted_value == expectedProtoValue

        where:
        suite              | avroType  | protoDescriptor                                        | avroValue                                                              | expectedProtoValue         | transformResult
        "array-primitives" | "string"  | ArrayStringProto.ArrayPrimitivesString.getDescriptor() | []                                                                     | []                         | { c -> c }
        "array-primitives" | "int"     | ArrayInt32Proto.ArrayPrimitivesInt32.getDescriptor()   | []                                                                     | []                         | { c -> c }
        "array-primitives" | "long"    | ArrayInt64Proto.ArrayPrimitivesInt64.getDescriptor()   | []                                                                     | []                         | { c -> c }
        "array-primitives" | "boolean" | ArrayInt64Proto.ArrayPrimitivesInt64.getDescriptor()   | []                                                                     | []                         | { c -> c }
        "array-primitives" | "boolean" | ArrayBoolProto.ArrayPrimitivesBool.getDescriptor()     | []                                                                     | []                         | { c -> c }
        "array-primitives" | "bytes"   | ArrayBytesProto.ArrayPrimitivesBytes.getDescriptor()   | []                                                                     | []                         | { c -> c }
        "array-primitives" | "float"   | ArrayFloatProto.ArrayPrimitivesFloat.getDescriptor()   | []                                                                     | []                         | { c -> c }
        "array-primitives" | "double"  | ArrayDoubleProto.ArrayPrimitivesDouble.getDescriptor() | []                                                                     | []                         | { c -> c }
        "array-primitives" | "string"  | ArrayStringProto.ArrayPrimitivesString.getDescriptor() | ["a", "b", "c"]                                                        | ["a", "b", "c"]            | { c -> c }
        "array-primitives" | "int"     | ArrayInt32Proto.ArrayPrimitivesInt32.getDescriptor()   | [1, 2, 3]                                                              | [1, 2, 3]                  | { c -> c }
        "array-primitives" | "long"    | ArrayInt64Proto.ArrayPrimitivesInt64.getDescriptor()   | [4l, 5l, 6l]                                                           | [4l, 5l, 6l]               | { c -> c }
        "array-primitives" | "boolean" | ArrayBoolProto.ArrayPrimitivesBool.getDescriptor()     | [true, true, false]                                                    | [true, true, false]        | { c -> c }
        "array-primitives" | "bytes"   | ArrayBytesProto.ArrayPrimitivesBytes.getDescriptor()   | [ByteBuffer.wrap("123".getBytes()), ByteBuffer.wrap("456".getBytes())] | ["123".bytes, "456".bytes] | { c -> c.bytes }
        "array-primitives" | "float"   | ArrayFloatProto.ArrayPrimitivesFloat.getDescriptor()   | [3.14f, 6.28f]                                                         | [3.14f, 6.28f]             | { c -> c }
        "array-primitives" | "double"  | ArrayDoubleProto.ArrayPrimitivesDouble.getDescriptor() | [3.14d, 6.28d]                                                         | [3.14d, 6.28d]             | { c -> c }
    }

    @Test
    void 'should convert map of primitive types'() {

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
        def converted_value = message.getField(field).toString().replace("\n", " ")
        converted_value == expectedProtoValue

        where:
        suite            | avroType  | protoDescriptor                                              | avroValue                                                                        | expectedProtoValue                                                               | transformResult
        "map-primitives" | "string"  | StringToStringProto.PrimitivesStringToString.getDescriptor() | [:]                                                                              | "[]"                                                                             | { c -> c }
        "map-primitives" | "int"     | StringToInt32Proto.PrimitivesStringToInt32.getDescriptor()   | [:]                                                                              | "[]"                                                                             | { c -> c }
        "map-primitives" | "long"    | StringToInt64Proto.PrimitivesStringToInt64.getDescriptor()   | [:]                                                                              | "[]"                                                                             | { c -> c }
        "map-primitives" | "boolean" | StringToInt64Proto.PrimitivesStringToInt64.getDescriptor()   | [:]                                                                              | "[]"                                                                             | { c -> c }
        "map-primitives" | "boolean" | StringToBoolProto.PrimitivesStringToBool.getDescriptor()     | [:]                                                                              | "[]"                                                                             | { c -> c }
        "map-primitives" | "bytes"   | StringToBytesProto.PrimitivesStringToBytes.getDescriptor()   | [:]                                                                              | "[]"                                                                             | { c -> c }
        "map-primitives" | "float"   | StringToFloatProto.PrimitivesStringToFloat.getDescriptor()   | [:]                                                                              | "[]"                                                                             | { c -> c }
        "map-primitives" | "double"  | StringToDoubleProto.PrimitivesStringToDouble.getDescriptor() | [:]                                                                              | "[]"                                                                             | { c -> c }
        "map-primitives" | "string"  | StringToStringProto.PrimitivesStringToString.getDescriptor() | ["a": "a", "b": "b", "c": "c"]                                                   | "[key: \"a\" value: \"a\" , key: \"b\" value: \"b\" , key: \"c\" value: \"c\" ]" | { c -> c }
        "map-primitives" | "int"     | StringToInt32Proto.PrimitivesStringToInt32.getDescriptor()   | ["a": 1, "b": 2, "c": 3]                                                         | "[key: \"a\" value: 1 , key: \"b\" value: 2 , key: \"c\" value: 3 ]"             | { c -> c }
        "map-primitives" | "long"    | StringToInt64Proto.PrimitivesStringToInt64.getDescriptor()   | ["a": 4l, "b": 5l, "c": 6l]                                                      | "[key: \"a\" value: 4 , key: \"b\" value: 5 , key: \"c\" value: 6 ]"             | { c -> c }
        "map-primitives" | "boolean" | StringToBoolProto.PrimitivesStringToBool.getDescriptor()     | ["a": true, "b": true, "c": false]                                               | "[key: \"a\" value: true , key: \"b\" value: true , key: \"c\" value: false ]"   | { c -> c }
        "map-primitives" | "bytes"   | StringToBytesProto.PrimitivesStringToBytes.getDescriptor()   | ["a": ByteBuffer.wrap("123".getBytes()), "b": ByteBuffer.wrap("456".getBytes())] | "[key: \"a\" value: \"123\" , key: \"b\" value: \"456\" ]"                       | { c -> c }
        "map-primitives" | "float"   | StringToFloatProto.PrimitivesStringToFloat.getDescriptor()   | ["a": 3.14f, "b": 6.28f]                                                         | "[key: \"a\" value: 3.14 , key: \"b\" value: 6.28 ]"                             | { c -> c }
        "map-primitives" | "double"  | StringToDoubleProto.PrimitivesStringToDouble.getDescriptor() | ["a": 3.14d, "b": 6.28d]                                                         | "[key: \"a\" value: 3.14 , key: \"b\" value: 6.28 ]"                             | { c -> c }

    }
}
