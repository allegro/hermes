package pl.allegro.tech.hermes.frontend.publishing.message

import groovy.json.JsonOutput
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage
import pl.allegro.tech.hermes.schema.CompiledSchema
import pl.allegro.tech.hermes.test.helper.avro.AvroUser
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader
import spock.lang.Specification

import java.nio.ByteBuffer

import static java.util.Collections.emptyMap

class MessageToJsonConverterTest extends Specification {

    def 'should convert avro to JSON'() {
        given:
        def compiledSchema = CompiledSchema.of(AvroUserSchemaLoader.load("/schema/user_4.avsc"), 1, 1)
        def avroUser = new AvroUser(compiledSchema, 'name', 16, 'favourite-colour')
        avroUser.add("accountBalance", ByteBuffer.wrap(29.99.unscaledValue().toByteArray()))

        when:
        def converted = new MessageToJsonConverter().convert(new AvroMessage(
                'message-id', avroUser.asBytes(), 0L, avroUser.compiledSchema, "partition-key", emptyMap()), false)

        then:
        new String(converted) == JsonOutput.toJson(
                [__metadata: null, name: 'name', age: 16, favoriteColor: 'favourite-colour', accountBalance: '29.99'])
    }

    def 'should return bytes when decoding fails'() {
        given:
        def avroUser = new AvroUser('name', 16, 'favourite-colour')

        when:
        def converted = new MessageToJsonConverter().convert(new AvroMessage(
                'message-id', 'unable-to-decode'.getBytes(), 0L, avroUser.compiledSchema, null, emptyMap()), false)

        then:
        new String(converted) == 'unable-to-decode'
    }

    def 'should return the same when no avro provided'() {
        when:
        def converted = new MessageToJsonConverter().convert(new JsonMessage(
                'message-id', 'given-message'.bytes, 0L, null, emptyMap()), false)

        then:
        new String(converted) == 'given-message'
    }
}
