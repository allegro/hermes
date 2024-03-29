package pl.allegro.tech.hermes.frontend.publishing.message

import groovy.json.JsonOutput
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage
import pl.allegro.tech.hermes.test.helper.avro.AvroUser
import spock.lang.Specification

import static java.util.Collections.emptyMap

class MessageToJsonConverterTest extends Specification {

    def 'should convert avro to JSON'() {
        given:
        def avroUser = new AvroUser('name', 16, 'favourite-colour')

        when:
        def converted = new MessageToJsonConverter().convert(new AvroMessage(
                'message-id', avroUser.asBytes(), 0L, avroUser.compiledSchema, "partition-key", emptyMap()), false)

        then:
        new String(converted) == JsonOutput.toJson(
                [__metadata: null, name: 'name', age: 16, favoriteColor: 'favourite-colour'])
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
