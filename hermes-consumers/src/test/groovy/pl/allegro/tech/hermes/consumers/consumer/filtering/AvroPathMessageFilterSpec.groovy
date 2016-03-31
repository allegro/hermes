package pl.allegro.tech.hermes.consumers.consumer.filtering

import com.google.common.collect.ImmutableList
import org.apache.avro.generic.GenericData
import pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader
import spock.lang.Specification
import wandou.avpath.Parser

import java.util.regex.Pattern

class AvroPathMessageFilterSpec extends Specification {

    def "avro path message filter should not pass for long"() {
        given:
        def schema = AvroUserSchemaLoader.load("/Record.avsc")
        GenericData.Record record = new GenericData.Record(schema)
        record.put("id", "foo")
        record.put("number", 150L)
        record.put("values", ImmutableList.of("Nexus5a", "Nexus6"))

        def bytes = AvroRecordToBytesConverter.recordToBytes(record, schema)

        Parser.PathSyntax ast = new Parser().parse(".number")

        def filter = new AvroPathMessageFilter(ast, Pattern.compile("10"))

        def message = MessageBuilder.withTestMessage().withContent(bytes).withSchema(schema, 0).build()

        expect:
        !filter.test(message)
    }


    def "avro path message filter should pass for long"() {
        given:
        def schema = AvroUserSchemaLoader.load("/Record.avsc")
        GenericData.Record record = new GenericData.Record(schema)
        record.put("id", "foo")
        record.put("number", 10L)
        record.put("values", ImmutableList.of("Nexus5a", "Nexus6"))

        def bytes = AvroRecordToBytesConverter.recordToBytes(record, schema)

        Parser.PathSyntax ast = new Parser().parse(".number")

        def filter = new AvroPathMessageFilter(ast, Pattern.compile("10"))

        def message = MessageBuilder.withTestMessage().withContent(bytes).withSchema(schema, 0).build()

        expect:
        filter.test(message)
    }

    def "avro path message filter should not pass array"() {
        given:
        def schema = AvroUserSchemaLoader.load("/Record.avsc")
        GenericData.Record record = new GenericData.Record(schema)
        record.put("id", "foo")
        record.put("number", 10L)
        record.put("values", ImmutableList.of("Nexus5a", "Nexus6"))

        def bytes = AvroRecordToBytesConverter.recordToBytes(record, schema)

        Parser.PathSyntax ast = new Parser().parse(".values")

        def filter = new AvroPathMessageFilter(ast, Pattern.compile("Nexus5.*"))

        def message = MessageBuilder.withTestMessage().withContent(bytes).withSchema(schema, 0).build()

        expect:
        !filter.test(message)
    }


    def "avro path message filter should pass array"() {
        given:
        def schema = AvroUserSchemaLoader.load("/Record.avsc")
        GenericData.Record record = new GenericData.Record(schema)
        record.put("id", "foo")
        record.put("number", 10L)
        record.put("values", ImmutableList.of("iPhone 5s", "iPhone SE"))

        def bytes = AvroRecordToBytesConverter.recordToBytes(record, schema)

        Parser.PathSyntax ast = new Parser().parse(".values")

        def filter = new AvroPathMessageFilter(ast, Pattern.compile("iPhone.*"))

        def message = MessageBuilder.withTestMessage().withContent(bytes).withSchema(schema, 0).build()

        expect:
        filter.test(message)
    }

    def "avro path message filter should pass"() {
        given:
        def schema = AvroUserSchemaLoader.load("/Record.avsc")
        GenericData.Record record = new GenericData.Record(schema)
        record.put("id", "foo")
        record.put("number", 10L)
        record.put("values", ImmutableList.of("a", "b"))

        def bytes = AvroRecordToBytesConverter.recordToBytes(record, schema)

        Parser.PathSyntax ast = new Parser().parse(".id");

        def filter = new AvroPathMessageFilter(ast, Pattern.compile("fo.*"))

        def message = MessageBuilder.withTestMessage().withContent(bytes).withSchema(schema, 0).build()

        expect:
        filter.test(message)
    }

    def "avro path message filter should not pass"() {
        given:
        def schema = AvroUserSchemaLoader.load("/Record.avsc")
        GenericData.Record record = new GenericData.Record(schema)
        record.put("id", "boo")
        record.put("number", 10L)
        record.put("values", ImmutableList.of("a", "b"))

        def bytes = AvroRecordToBytesConverter.recordToBytes(record, schema)

        Parser.PathSyntax ast = new Parser().parse(".id");

        def filter = new AvroPathMessageFilter(ast, Pattern.compile("fo.*"))

        def message = MessageBuilder.withTestMessage().withContent(bytes).withSchema(schema, 0).build()

        expect:
        !filter.test(message)
    }

}


