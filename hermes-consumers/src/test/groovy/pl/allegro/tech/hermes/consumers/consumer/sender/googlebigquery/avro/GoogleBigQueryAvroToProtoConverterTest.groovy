package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro

import com.google.cloud.bigquery.storage.v1.TableFieldSchema
import com.google.cloud.bigquery.storage.v1.TableSchema
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.junit.jupiter.api.Test
import pl.allegro.tech.hermes.consumers.consumer.Message
import spock.lang.Specification
import spock.lang.Unroll
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.AvroTrait
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.*

import java.nio.ByteBuffer

class GoogleBigQueryAvroToProtoConverterTest extends Specification implements AvroTrait {
    @Test
    void convertPrimitivesToProtoMessage() {

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
        record.hasField(fieldName) == message.hasField(field)
        transformResult(converted_value) == expectedProtoValue


        where:
        suite        | avroType  | protoDescriptor                              | avroValue                         | expectedProtoValue | transformResult
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | "value"                           | "value"            | { c -> c }
        "primitives" | "int"     | Int32Proto.PrimitivesInt32.getDescriptor()   | 12                                | 12                 | { c -> c }
        "primitives" | "long"    | Int64Proto.PrimitivesInt64.getDescriptor()   | 12l                               | 12l                | { c -> c }
        "primitives" | "boolean" | Int64Proto.PrimitivesInt64.getDescriptor()   | 12l                               | 12l                | { c -> c }
/*-->*/ "primitives" | "boolean" | BoolProto.PrimitivesBool.getDescriptor()     | true                              | true               | { c -> c }
        "primitives" | "bytes"   | BytesProto.PrimitivesBytes.getDescriptor()   | ByteBuffer.wrap("123".getBytes()) | "123"              | { c -> new String(c.bytes) }
        "primitives" | "float"   | FloatProto.PrimitivesFloat.getDescriptor()   | 1.234f                            | 1.234f             | { c -> c }
        "primitives" | "double"  | DoubleProto.PrimitivesDouble.getDescriptor() | 1.234d                            | 1.234d             | { c -> c }


    }

    void convertPrimitivesToProtoMessageWithCasting() {

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
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | "value"   | "value"            | { c -> c }
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | 12        | "12"               | { c -> c }
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | 12l       | "12"               | { c -> c }
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | 12f       | "12.0"             | { c -> c }
        "primitives" | "string"  | StringProto.PrimitivesString.getDescriptor() | 12d       | "12.0"             | { c -> c }

        "primitives" | "int"     | Int32Proto.PrimitivesInt32.getDescriptor()   | 12        | 12                 | { c -> c }
        "primitives" | "long"    | Int32Proto.PrimitivesInt32.getDescriptor()   | 12l       | 12                 | { c -> c }
        "primitives" | "float"   | Int32Proto.PrimitivesInt32.getDescriptor()   | 12f       | 12                 | { c -> c }
        "primitives" | "double"  | Int32Proto.PrimitivesInt32.getDescriptor()   | 12d       | 12                 | { c -> c }
        "primitives" | "string"  | Int32Proto.PrimitivesInt32.getDescriptor()   | "12"      | 12                 | { c -> c }
        "primitives" | "boolean" | Int32Proto.PrimitivesInt32.getDescriptor()   | true      | 1                  | { c -> c }
        "primitives" | "boolean" | Int32Proto.PrimitivesInt32.getDescriptor()   | false     | 0                  | { c -> c }
//
        "primitives" | "int"     | Int64Proto.PrimitivesInt64.getDescriptor()   | 12        | 12l                | { c -> c }
        "primitives" | "long"    | Int64Proto.PrimitivesInt64.getDescriptor()   | 12l       | 12l                | { c -> c }
        "primitives" | "float"   | Int64Proto.PrimitivesInt64.getDescriptor()   | 12f       | 12l                | { c -> c }
        "primitives" | "double"  | Int64Proto.PrimitivesInt64.getDescriptor()   | 12d       | 12l                | { c -> c }
        "primitives" | "string"  | Int64Proto.PrimitivesInt64.getDescriptor()   | "12.0"    | 12l                | { c -> c }
        "primitives" | "boolean" | Int64Proto.PrimitivesInt64.getDescriptor()   | true      | 1l                 | { c -> c }
        "primitives" | "boolean" | Int64Proto.PrimitivesInt64.getDescriptor()   | false     | 0l                 | { c -> c }

        "primitives" | "int"     | FloatProto.PrimitivesFloat.getDescriptor()   | 12        | 12.0f              | { c -> c }
        "primitives" | "long"    | FloatProto.PrimitivesFloat.getDescriptor()   | 12l       | 12.0f              | { c -> c }
        "primitives" | "float"   | FloatProto.PrimitivesFloat.getDescriptor()   | 12.234f   | 12.234f            | { c -> c }
        "primitives" | "double"  | FloatProto.PrimitivesFloat.getDescriptor()   | 12.234d   | 12.234f            | { c -> c }
        "primitives" | "string"  | FloatProto.PrimitivesFloat.getDescriptor()   | "12.234"  | 12.234f            | { c -> c }
        "primitives" | "boolean" | FloatProto.PrimitivesFloat.getDescriptor()   | true      | 1f                 | { c -> c }
        "primitives" | "boolean" | FloatProto.PrimitivesFloat.getDescriptor()   | false     | 0f                 | { c -> c }

        "primitives" | "int"     | DoubleProto.PrimitivesDouble.getDescriptor() | 12        | 12.0d              | { c -> c }
        "primitives" | "long"    | DoubleProto.PrimitivesDouble.getDescriptor() | 12l       | 12.0d              | { c -> c }
        "primitives" | "float"   | DoubleProto.PrimitivesDouble.getDescriptor() | 12.5f     | 12.5d              | { c -> c }
        "primitives" | "double"  | DoubleProto.PrimitivesDouble.getDescriptor() | 12.234d   | 12.234d            | { c -> c }
        "primitives" | "string"  | DoubleProto.PrimitivesDouble.getDescriptor() | "12.234"  | 12.234d            | { c -> c }
        "primitives" | "boolean" | DoubleProto.PrimitivesDouble.getDescriptor() | true      | 1d                 | { c -> c }
        "primitives" | "boolean" | DoubleProto.PrimitivesDouble.getDescriptor() | false     | 0d                 | { c -> c }

        "primitives" | "boolean" | BoolProto.PrimitivesBool.getDescriptor()     | true      | true               | { c -> c }
        "primitives" | "boolean" | BoolProto.PrimitivesBool.getDescriptor()     | false     | false              | { c -> c }
        "primitives" | "string"  | BoolProto.PrimitivesBool.getDescriptor()     | "true"    | true               | { c -> c }
        "primitives" | "string"  | BoolProto.PrimitivesBool.getDescriptor()     | "false"   | false              | { c -> c }
        "primitives" | "boolean" | BoolProto.PrimitivesBool.getDescriptor()     | 1         | true               | { c -> c }
        "primitives" | "boolean" | BoolProto.PrimitivesBool.getDescriptor()     | 0         | false              | { c -> c }
    }

    @Test
    void convertNullablePrimitivesToProtoMessage() {

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
//        record.hasField(fieldName) == message.hasField(field)
        def converted_value = message.hasField(field) ? (message.getField(field)) : null
        transformResult(converted_value) == expectedProtoValue


        where:
        suite                 | avroType  | protoDescriptor                                       | avroValue                         | expectedProtoValue | transformResult
        "nullable-primitives" | "long"    | NInt64Proto.NullablePrimitivesInt64.getDescriptor()   | 12                                | 12l                | { c -> c }
        "nullable-primitives" | "string"  | NStringProto.NullablePrimitivesString.getDescriptor() | null                              | null               | { c -> c }
        "nullable-primitives" | "string"  | NStringProto.NullablePrimitivesString.getDescriptor() | "value"                           | "value"            | { c -> c }
        "nullable-primitives" | "int"     | NInt32Proto.NullablePrimitivesInt32.getDescriptor()   | 12                                | 12                 | { c -> c }
        "nullable-primitives" | "long"    | NInt64Proto.NullablePrimitivesInt64.getDescriptor()   | 12l                               | 12l                | { c -> c }
        "nullable-primitives" | "boolean" | NInt64Proto.NullablePrimitivesInt64.getDescriptor()   | 12l                               | 12l                | { c -> c }
/*-->*/ "nullable-primitives" | "boolean" | NBoolProto.NullablePrimitivesBool.getDescriptor()     | true                              | true               | { c -> c }
        "nullable-primitives" | "bytes"   | NBytesProto.NullablePrimitivesBytes.getDescriptor()   | ByteBuffer.wrap("123".getBytes()) | "123"              | { c -> new String(c.bytes) }
        "nullable-primitives" | "float"   | NFloatProto.NullablePrimitivesFloat.getDescriptor()   | 1.234f                            | 1.234f             | { c -> c }
        "nullable-primitives" | "double"  | NDoubleProto.NullablePrimitivesDouble.getDescriptor() | 1.234d                            | 1.234d             | { c -> c }


    }

    @Test
    void convertNullableWithNullPrimitivesToProtoMessage() {

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
        record.hasField(fieldName) == !message.hasField(field)
        def converted_value = message.hasField(field) ? (message.getField(field)) : null
        transformResult(converted_value) == expectedProtoValue


        where:
        suite                 | avroType  | protoDescriptor                                       | avroValue | expectedProtoValue | transformResult
        "nullable-primitives" | "string"  | NStringProto.NullablePrimitivesString.getDescriptor() | null      | null               | { c -> c }
        "nullable-primitives" | "int"     | NInt32Proto.NullablePrimitivesInt32.getDescriptor()   | null      | null               | { c -> c }
        "nullable-primitives" | "long"    | NInt64Proto.NullablePrimitivesInt64.getDescriptor()   | null      | null               | { c -> c }
        "nullable-primitives" | "boolean" | NInt64Proto.NullablePrimitivesInt64.getDescriptor()   | null      | null               | { c -> c }
/*-->*/ "nullable-primitives" | "boolean" | NBoolProto.NullablePrimitivesBool.getDescriptor()     | null      | null               | { c -> c }
        "nullable-primitives" | "bytes"   | NBytesProto.NullablePrimitivesBytes.getDescriptor()   | null      | null               | { c -> c }
        "nullable-primitives" | "float"   | NFloatProto.NullablePrimitivesFloat.getDescriptor()   | null      | null               | { c -> c }
        "nullable-primitives" | "double"  | NDoubleProto.NullablePrimitivesDouble.getDescriptor() | null      | null               | { c -> c }


    }

    @Test
    void convertDefaultPrimitivesToProtoMessage() {

        given:
        Schema schema = getSchemaFromResources("${suite}/${avroType}")
        String fieldName = "field"
        GenericRecord record = fieldExists ? new GenericRecordBuilder(schema).set(fieldName, null).build() : new GenericRecordBuilder(schema).build()
        GoogleBigQueryAvroToProtoConverter converter = new GoogleBigQueryAvroToProtoConverter()

        when:
        DynamicMessage message = converter.convertToProtoMessage(protoDescriptor, record)


        then:
        message != null
        println(message)
        def field = message.getDescriptorForType().findFieldByName(fieldName)
//        record.hasField(fieldName) == !message.hasField(field)
        def converted_value = message.hasField(field) ? (message.getField(field)) : null
        transformResult(converted_value) == expectedProtoValue


        where:
        suite                | avroType  | fieldExists | protoDescriptor                                       | expectedProtoValue                             | transformResult
        "default-primitives" | "string"  | true        | NStringProto.NullablePrimitivesString.getDescriptor() | null                                           | { c -> c }
        "default-primitives" | "int"     | true        | NInt32Proto.NullablePrimitivesInt32.getDescriptor()   | null                                           | { c -> c }
        "default-primitives" | "long"    | true        | NInt64Proto.NullablePrimitivesInt64.getDescriptor()   | null                                           | { c -> c }
        "default-primitives" | "boolean" | true        | NInt64Proto.NullablePrimitivesInt64.getDescriptor()   | null                                           | { c -> c }
        "default-primitives" | "boolean" | true        | NBoolProto.NullablePrimitivesBool.getDescriptor()     | null                                           | { c -> c }
        "default-primitives" | "bytes"   | true        | NBytesProto.NullablePrimitivesBytes.getDescriptor()   | null                                           | { c -> c }
        "default-primitives" | "float"   | true        | NFloatProto.NullablePrimitivesFloat.getDescriptor()   | null                                           | { c -> c }
        "default-primitives" | "double"  | true        | NDoubleProto.NullablePrimitivesDouble.getDescriptor() | null                                           | { c -> c }
        "default-primitives" | "string"  | false       | NStringProto.NullablePrimitivesString.getDescriptor() | "Z sejmu dla Faktów Katarzyna Bolesna-Mordęga" | { c -> c }
        "default-primitives" | "int"     | false       | NInt32Proto.NullablePrimitivesInt32.getDescriptor()   | 997                                            | { c -> c }
        "default-primitives" | "long"    | false       | NInt64Proto.NullablePrimitivesInt64.getDescriptor()   | 1614322339997                                  | { c -> c }
        "default-primitives" | "boolean" | false       | NBoolProto.NullablePrimitivesBool.getDescriptor()     | true                                           | { c -> c }
        "default-primitives" | "bytes"   | false       | NBytesProto.NullablePrimitivesBytes.getDescriptor()   | "\u0074\u0065\u0073\u0074".getBytes()          | { c -> c.bytes }
        "default-primitives" | "float"   | false       | NFloatProto.NullablePrimitivesFloat.getDescriptor()   | 3.14f                                          | { c -> c }
        "default-primitives" | "double"  | false       | NDoubleProto.NullablePrimitivesDouble.getDescriptor() | 3.14d                                          | { c -> c }


    }

    @Test
    void convertArrayPrimitivesToProtoMessage() {

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
    void convertMapPrimitivesToProtoMessage() {

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

    @Test
    void convertRecordPrimitivesToProtoMessage() {

        given:
        Schema schema = getSchemaFromResources("${suite}/${avroType}")
        String fieldName = "field"
        String valueFieldName = "value_field"
//        GenericRecord record = fieldExists ? new GenericRecordBuilder(schema).set(fieldName, null).build() : new GenericRecordBuilder(schema).build()
        GenericRecord record = new GenericRecordBuilder(schema).set(
                fieldName,
                new GenericRecordBuilder(schema.getField(fieldName).schema()).set(valueFieldName, avroValue).build()
        ).build()
        GoogleBigQueryAvroToProtoConverter converter = new GoogleBigQueryAvroToProtoConverter()

        when:
        DynamicMessage message = converter.convertToProtoMessage(protoDescriptor, record)


        then:
        message != null
        println(message)
        def field = message.getDescriptorForType().findFieldByName(fieldName)
        def convertedFieldValue = message.getField(field).toString().replace("\\", "").replace("\n", "")
        def expectedRecord = "value_field: ${expectedProtoValue}".replace("\\", "").replace("\n", "")
//        record.hasField(fieldName) == !message.hasField(field)def converted_value = message.hasField(field) ? (message.getField(field)) : null
        expectedRecord == convertedFieldValue


        where:
        suite               | avroType  | protoDescriptor                                          | avroValue                         | expectedProtoValue | transformResult
        "record-primitives" | "string"  | RecordStringProto.RecordPrimitivesString.getDescriptor() | "value"                           | "\"value\""        | { c -> c }
        "record-primitives" | "int"     | RecordInt32Proto.RecordPrimitivesInt32.getDescriptor()   | 12                                | 12                 | { c -> c }
        "record-primitives" | "long"    | RecordInt64Proto.RecordPrimitivesInt64.getDescriptor()   | 12l                               | 12l                | { c -> c }
        "record-primitives" | "boolean" | RecordInt64Proto.RecordPrimitivesInt64.getDescriptor()   | 12l                               | 12l                | { c -> c }
/*-->*/ "record-primitives" | "boolean" | RecordBoolProto.RecordPrimitivesBool.getDescriptor()     | true                              | true               | { c -> c }
        "record-primitives" | "bytes"   | RecordBytesProto.RecordPrimitivesBytes.getDescriptor()   | ByteBuffer.wrap("123".getBytes()) | "\"123\""              | { c -> new String(c.bytes) }
        "record-primitives" | "float"   | RecordFloatProto.RecordPrimitivesFloat.getDescriptor()   | 1.234f                            | 1.234f             | { c -> c }
        "record-primitives" | "double"  | RecordDoubleProto.RecordPrimitivesDouble.getDescriptor() | 1.234d                            | 1.234d             | { c -> c }


    }
}
