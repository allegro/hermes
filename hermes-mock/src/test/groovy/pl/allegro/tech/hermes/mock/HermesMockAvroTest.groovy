package pl.allegro.tech.hermes.mock

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.avro.Schema
import org.apache.avro.reflect.ReflectData
import org.junit.ClassRule
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher
import spock.lang.Shared
import spock.lang.Specification
import tech.allegro.schema.json2avro.converter.JsonAvroConverter

class HermesMockAvroTest extends Specification {
    @ClassRule
    @Shared
    HermesMockRule hermes = new HermesMockRule(56789)

    Schema schema = ReflectData.get().getSchema(TestMessage)

    JsonAvroConverter jsonAvroConverter = new JsonAvroConverter()

    HermesPublisher publisher = new HermesPublisher("http://localhost:56789")

    def setup() {
        hermes.resetReceivedRequest()
    }

    def "should receive an Avro message"() {
        given:
            def topicName = "my-test-avro-topic"
            hermes.define().avroTopic(topicName)

        when:
            publish(topicName)

        then: "check for any single message on the topic and check for single specific avro message"
            hermes.expect().singleMessageOnTopic(topicName)
            hermes.expect().singleAvroMessageOnTopic(topicName, schema)
    }

    def "should get all messages as avro"() {
        given:
            def topicName = "get-all-avro-topic"
            hermes.define().avroTopic(topicName)

            def messages = (1..5).collect { new TestMessage("key-" + it, "value-" + it) }

        when:
            messages.each { publish(topicName, it) }

        then:
            hermes.expect().avroMessagesOnTopic(topicName, 5, schema)
    }

    def "should get messages with schema from file"() {
        given:
            def topicName = "my-avro-from-file"
            hermes.define().avroTopic(topicName)
            def schema = AvroUserSchemaLoader.load("/msg.avsc")
            def json = '''
            {
                "id": "0001-whatever",
                "type": "testing-message",
                "value": 0.42
            }
        '''
            def message = new ObjectMapper().readValue(json, HashMap.class)
            def avro = new JsonAvroConverter().convertToAvro(json.bytes, schema)

        when:
            publish(topicName, avro)

        then:
            def received = hermes.query().lastAvroMessageAs(topicName, schema, HashMap.class).get()
            message == received
    }

    def "should throw on incorrect Avro schema"() {
        given:
            def schema = ReflectData.get().getSchema(TestMessage)
            def topicName = "my-first-avro-failing-test-topic"
            hermes.define().avroTopic(topicName)

        when:
            publish(topicName, "whatever".bytes)

        and:
            hermes.query().lastAvroMessageAs(topicName, schema, HashMap.class)

        then:
            def ex = thrown(HermesMockException)
            ex.message.startsWith("Cannot convert body")
    }

    def "should get all avro message with schema"() {
        given:
            def topicName = "get-all-test-topic"
            hermes.define().avroTopic(topicName)

            def messages = (1..5).collect() { new TestMessage("key-" + it, "value-" + it) }

        when:
            messages.each { publish(topicName, it) }

        then:
            def requests = hermes.query().allAvroMessagesAs(topicName, schema, TestMessage)
            requests.size() == 5
            requests[0].key == "key-1"
            requests[0].value == "value-1"
    }

    def "should get all avro message as raw bytes"() {
        given:
            def topicName = "get-all-test-topic"
            hermes.define().avroTopic(topicName)

            def messages = (1..5).collect() { new TestMessage("key-" + it, "value-" + it) }

        when:
            messages.each { publish(topicName, it) }

        then:
            def requests = hermes.query().allAvroRawMessages(topicName)
            requests.size() == 5
            requests[0] == asAvro(messages[0])
    }

    def "should get last message as raw bytes"() {
        given:
            def topicName = "my-topic"
            hermes.define().avroTopic(topicName)
            def count = 3
            def messages = (1..count).collect { new TestMessage("key-" + it, "value-" + it) }

        when:
            messages.each { publish(topicName, it) }

        then:
            def message = hermes.query().lastAvroRawMessage(topicName)
            message.isPresent()
            message.get() == new JsonAvroConverter().convertToAvro(messages[count - 1].asJson().bytes, schema)

            def request = hermes.query().lastRequest(topicName)
            request.isPresent()
    }

    def asAvro(TestMessage message) {
        return jsonAvroConverter.convertToAvro(message.asJson().bytes, schema)
    }

    def publish(String topic) {
        publish(topic, TestMessage.random())
    }

    def publish(String topic, TestMessage message) {
        publish(topic, asAvro(message))
    }

    def publish(String topic, byte[] avro) {
        publisher.publish(topic, avro)
    }
}
