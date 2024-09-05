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
        record.hasField(fieldName) == message.hasField(field)
        def converted_value = message.hasField(field) ? (message.getField(field)) : null
        transformResult(converted_value) == expectedProtoValue


        where:
        suite                 | avroType  | protoDescriptor                                       | avroValue                         | expectedProtoValue | transformResult
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

}
