package pl.allegro.tech.hermes.mock

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.avro.Schema
import org.apache.avro.reflect.ReflectData
import org.apache.http.HttpStatus
import org.junit.ClassRule
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Shared
import spock.lang.Specification
import tech.allegro.schema.json2avro.converter.JsonAvroConverter

import java.time.Duration
import java.time.Instant

import static java.time.Instant.now
import static pl.allegro.tech.hermes.mock.exchange.Response.Builder.aResponse

class HermesMockAvroTest extends Specification {

    @Shared
    int port = Ports.nextAvailable()

    @ClassRule
    @Shared
    HermesMockRule hermes = new HermesMockRule(port)

    Schema schema = ReflectData.get().getSchema(TestMessage)
    Schema differentSchema = ReflectData.get().getSchema(TestMessageWithDifferentSchema)

    JsonAvroConverter jsonAvroConverter = new JsonAvroConverter()

    HermesPublisher publisher = new HermesPublisher("http://localhost:$port")

    def setup() {
        hermes.resetReceivedRequest()
        hermes.resetMappings()
    }

    def "should receive an Avro message matched by pattern different schema"() {
        given: "define wiremock responses for 2 topics with different schemas"
            def topicName = "my-test-avro-topic-1"
            def topicName2 = "my-test-avro-topic-2"

            hermes.define().avroTopic(topicName,
                    aResponse().withStatusCode(201).build(),
                    schema,
                    TestMessage,
                    { it -> it.key == "test-key-pattern" })

            hermes.define().avroTopic(topicName2,
                    aResponse().withStatusCode(201).build(),
                    differentSchema,
                    TestMessageWithDifferentSchema,
                    { it -> it.value.contains(7) })

        when: "messages with matching patterns are published on topics"
            def message = new TestMessage("test-key-pattern", "test-key-value")
            def message2 = new TestMessageWithDifferentSchema("test-key-name", 7)

            def response = publish(topicName, message)
            def response2 = publish(topicName2, message2)

        then: "check for any single message on the topics and check for correct responses"
            hermes.expect().singleMessageOnTopic(topicName)
            hermes.expect().singleMessageOnTopic(topicName2)

            response.status == HttpStatus.SC_CREATED
            response2.status == HttpStatus.SC_CREATED
    }


    def "should receive an Avro message matched by pattern"() {
        given: "define wiremock response for matching avro pattern"
            def topicName = "my-test-avro-topic"
            hermes.define().avroTopic(topicName,
                    aResponse().withStatusCode(201).build(),
                    schema,
                    TestMessage,
                    { it -> it.key == "test-key-pattern" })

        when: "message with matching pattern is published on topic"
            def message = new TestMessage("test-key-pattern", "test-key-value")
            def response = publish(topicName, message)

        then: "check for any single message on the topic and check for correct response"
            hermes.expect().singleMessageOnTopic(topicName)
            response.status == HttpStatus.SC_CREATED
    }

    def "should not match avro pattern"() {
        given: "define wiremock response for matching avro pattern"
            def topicName = "my-test-avro-topic"
            hermes.define().avroTopic(topicName,
                    aResponse().withStatusCode(201).build(),
                    schema,
                    TestMessage,
                    { it -> it.key == "non-existing-key" })

        when: "message with non-matching pattern is published on topic"
            def message = new TestMessage("test-key-pattern", "test-key-value")
            def response = publish(topicName, message)

        then: "check for correct response status"
            response.status == HttpStatus.SC_NOT_FOUND
    }

    def "should receive an Avro message"() {
        given:
            def topicName = "my-test-avro-topic"
            hermes.define().avroTopic(topicName)

        when:
            def response = publish(topicName)

        then: "check for any single message on the topic and check for single specific avro message"
            hermes.expect().singleMessageOnTopic(topicName)
            hermes.expect().singleAvroMessageOnTopic(topicName, schema)
            response.status == HttpStatus.SC_CREATED
    }

    def "should respond with a delay"() {
        given:
            def topicName = "my-test-avro-topic"
            Duration fixedDelay = Duration.ofMillis(500)
            hermes.define().avroTopic(topicName, aResponse().withFixedDelay(fixedDelay).build())
            Instant start = now()

        when:
            publish(topicName)

        then:
            hermes.expect().singleMessageOnTopic(topicName)
            hermes.expect().singleAvroMessageOnTopic(topicName, schema)
            Duration.between(start, now()) >= fixedDelay
    }

    def "should respond for a message send with delay"() {
        given:
            def topicName = "my-test-avro-topic"
            def delayInMillis = 2_000
            hermes.define().avroTopic(topicName, aResponse().build())

        when:
            Thread.start {
                Thread.sleep(delayInMillis)
                publish(topicName)
            }

        then:
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

    def "should get all filtered messages as avro"() {
        given:
            def topicName = "get-all-filtered-avro-topic"
            hermes.define().avroTopic(topicName)

            def messages = (1..5).collect { new TestMessage("key-" + it, "value-" + it) }
            def filter = { TestMessage m -> m.key.startsWith("key-1") || m.key.startsWith("key-3") }

        when:
            messages.each { publish(topicName, it) }

        then:
            hermes.expect().avroMessagesOnTopic(topicName, 2, schema, TestMessage, filter)
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
            ex.message.startsWith("Cannot decode body")
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

    def "should return last message that matches filter"() {
        given:
            def topicName = "my-topic"
            hermes.define().avroTopic(topicName)
            def count = 3
            def messages = (1..count).collect { new TestMessage("key", "value-" + it) }
            def filter = { TestMessage m -> m.key.equals("key") }

        when:
            messages.each { publish(topicName, it) }

        then:
            def message = hermes.query().lastMatchingAvroMessageAs(topicName, schema, TestMessage, filter)
            message.isPresent()

            def msg = message.get() as TestMessage
            msg.key == messages[count - 1].key
            msg.value == messages[count - 1].value
    }

    def "should return proper number of message"() {
        given:
            def topicName = "my-topic"
            hermes.define().jsonTopic(topicName)
            def count = 3
            def messages = (1..count).collect { new TestMessage("key-" + it, "value-" + it) }

        when:
            messages.each { publish(topicName, it) }

        then:
            count == hermes.query().countAvroMessages(topicName)
    }

    def "should return proper number of matching messages"() {
        given:
            def topicName = "my-topic"
            hermes.define().jsonTopic(topicName)
            def messages = (1..3).collect { new TestMessage("key-" + it, "value-" + it) }
            def filter = { TestMessage m -> m.key.startsWith("key-1") }

        when:
            messages.each { publish(topicName, it) }
            5.times { publish(topicName) }

        then:
            1 == hermes.query().countMatchingAvroMessages(topicName, schema, TestMessage, filter)
    }

    def "should remove stub mapping for an Avro topic matched by pattern"() {
        given: "define wiremock response for two different matching avro patterns on the same topic"
        def topicName = "my-test-avro-topic"
        def pattern1 = "test-key-pattern-1"
        def pattern2 = "test-key-pattern-2"
        def value = "test-key-value"
        def avroTopicStub1 = hermes.define().avroTopic(topicName,
                aResponse().withStatusCode(HttpStatus.SC_CREATED).build(),
                schema,
                TestMessage,
                { it -> it.key == pattern1 })

        hermes.define().avroTopic(topicName,
                aResponse().withStatusCode(HttpStatus.SC_CREATED).build(),
                schema,
                TestMessage,
                { it -> it.key == pattern2 })

        when: "two messages with matching patterns are published on topic"
        def response1 = publish(topicName, new TestMessage(pattern1, value))
        def response2 = publish(topicName, new TestMessage(pattern2, value))

        then: "check for any single message on the topic and check for correct response"
        response1.status == HttpStatus.SC_CREATED
        response2.status == HttpStatus.SC_CREATED

        when: "first stub mapping is removed and two messages are sent again"
        hermes.define().removeStubMapping(avroTopicStub1)
        response1 = publish(topicName, new TestMessage(pattern1, value))
        response2 = publish(topicName, new TestMessage(pattern2, value))

        then: "removed stub mapping should response with not found, second stub on same topic should return 201 status"
        response1.status == HttpStatus.SC_NOT_FOUND
        response2.status == HttpStatus.SC_CREATED
    }

    def asAvro(TestMessage message) {
        return jsonAvroConverter.convertToAvro(message.asJson().bytes, schema)
    }

    def asAvro(TestMessageWithDifferentSchema message) {
        return jsonAvroConverter.convertToAvro(message.asJson().bytes, differentSchema)
    }

    def publish(String topic) {
        publish(topic, TestMessage.random())
    }

    def publish(String topic, TestMessage message) {
        publish(topic, asAvro(message))
    }

    def publish(String topic, TestMessageWithDifferentSchema message) {
        publish(topic, asAvro(message))
    }

    def publish(String topic, byte[] avro) {
        publisher.publish(topic, avro)
    }
}
