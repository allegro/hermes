package pl.allegro.tech.hermes.mock

import org.apache.avro.Schema
import org.apache.avro.reflect.ReflectData
import org.apache.http.HttpStatus
import org.junit.ClassRule
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Shared
import spock.lang.Specification
import tech.allegro.schema.json2avro.converter.JsonAvroConverter

class HermesMockTest extends Specification {

    @Shared
    int port = Ports.nextAvailable()

    @ClassRule
    @Shared
    HermesMockRule hermes = new HermesMockRule(port)

    HermesPublisher publisher = new HermesPublisher("http://localhost:$port")

    Schema schema = ReflectData.get().getSchema(TestMessage)

    JsonAvroConverter jsonAvroConverter = new JsonAvroConverter()

    def setup() {
        hermes.resetReceivedRequest()
    }

    def "should receive a message"() {
        given:
            def topicName = "my-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            def response = publishJson(topicName)

        then:
            hermes.expect().singleMessageOnTopic(topicName)
            response.status == HttpStatus.SC_CREATED
    }

    def "should receive 3 messages"() {
        given:
            def topicName = "my-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            3.times { publishJson(topicName) }

        then:
            hermes.expect().messagesOnTopic(topicName, 3)
    }

    def "should receive 2 messages + 1 delayed"() {
        given:
            def topicName = "my-test-topic-3"
            hermes.define().jsonTopic(topicName)

        when:
            2.times { publishJson(topicName) }

            Thread.start {
                sleep(100)
                publishJson(topicName)
            }

        then:
            hermes.expect().messagesOnTopic(topicName, 3)
    }

    def "should receive message as class"() {
        given:
            def topicName = "my-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            publishJson(topicName)

        then:
            hermes.expect().singleJsonMessageOnTopicAs(topicName, TestMessage)
    }

    def "should receive all messages as a particular class"() {
        given:
            def topicName = "my-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            3.times { publishJson(topicName) }
            3.times { publishJson("whatever") }

        then:
            hermes.expect().jsonMessagesOnTopicAs(topicName, 3, TestMessage)
    }

    def "should receive all filtered messages as a particular class"() {
        given:
            def topicName = "my-test-filtered-topic"
            hermes.define().jsonTopic(topicName)
            def messages = (1..5).collect { new TestMessage("key-" + it, "value-" + it) }
            def filter = { TestMessage m -> m.key.startsWith("key-1") || m.key.startsWith("key-3") }

        when:
            messages.each { publish(topicName, it.asJson()) }
            3.times { publishJson("whatever") }

        then:
            hermes.expect().jsonMessagesOnTopicAs(topicName, 2, TestMessage, filter)
    }

    def "should throw on more than 1 message"() {
        given:
            def topicName = "my-first-failing-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            2.times { publishJson(topicName) }

        and:
            hermes.expect().singleMessageOnTopic(topicName)

        then:
            def ex = thrown(HermesMockException)
            ex.message == "Hermes mock did not receive 1 messages."
    }

    def "should throw on more than 1 message of particular class"() {
        given:
            def topicName = "my-first-failing-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            2.times { publishJson(topicName) }

        and:
            hermes.expect().singleJsonMessageOnTopicAs(topicName, TestMessage)

        then:
            def ex = thrown(HermesMockException)
            ex.message == "Hermes mock did not receive 1 messages, got 2"
    }

    def "should get all messages"() {
        given:
            def topicName = "get-all-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            5.times { publishJson(topicName) }

        then:
            def requests = hermes.query().allRequests()
            requests.size() == 5
    }

    def 'should get all messages from topic'() {
        given:
            def topicName = "get-all-test-topic"
            def topicName2 = "get-all-test-topic-2"
            hermes.define().jsonTopic(topicName)
            hermes.define().jsonTopic(topicName2)

        when:
            5.times { publishJson(topicName) }
            2.times { publishJson(topicName2) }

        then:
            def requests1 = hermes.query().allRequestsOnTopic(topicName)
            requests1.size() == 5

        and:
            def requests2 = hermes.query().allRequestsOnTopic(topicName2)
            requests2.size() == 2

        and:
            hermes.query().allRequests().size() == 7
    }

    def 'should reset received messages'() {
        given:
            def topicName = "get-all-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            5.times { publishJson(topicName) }

        then:
            hermes.query().allRequestsOnTopic(topicName).size() == 5

        and:
            hermes.resetReceivedRequest()

        then:
            hermes.query().allRequestsOnTopic(topicName).isEmpty()
    }

    def "should get all messages as specified class"() {
        given:
            def topicName = "get-all-test-topic"
            hermes.define().jsonTopic(topicName)

            def messages = (1..5).collect { new TestMessage("key-" + it, "value-" + it) }

        when:
            messages.each { publish(topicName, it.asJson()) }

        then:
            def requests = hermes.query().allJsonMessagesAs(topicName, TestMessage)
            requests.size() == 5
            requests[0].key == "key-1"
            requests[0].value == "value-1"
    }

    def "should get last message as specified class"() {
        given:
            def topicName = "my-topic"
            hermes.define().jsonTopic(topicName)
            def count = 3
            def messages = (1..count).collect { new TestMessage("key-" + it, "value-" + it) }

        when:
            messages.each { publish(topicName, it.asJson()) }

        then:
            def message = hermes.query().lastJsonMessageAs(topicName, TestMessage)
            message.isPresent()
            message.get().key == "key-3"

            def request = hermes.query().lastRequest(topicName)
            request.isPresent()
            new String(request.get().getBody()) == message.get().asJson()
    }

    def "should return last message that matches filter"() {
        given:
            def topicName = "my-topic"
            hermes.define().avroTopic(topicName)
            def count = 3
            def messages = (1..count).collect { new TestMessage("key", "value-" + it) }
            def filter = { TestMessage m -> m.key.equals("key") }

        when:
            messages.each { publish(topicName, it.asJson()) }

        then:
            def message = hermes.query().lastMatchingJsonMessageAs(topicName, TestMessage, filter)
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
            messages.each { publish(topicName, it.asJson()) }

        then:
            count == hermes.query().countJsonMessages(topicName, TestMessage)
    }

    def "should return proper number of matching messages"() {
        given:
            def topicName = "my-topic"
            hermes.define().jsonTopic(topicName)
            def messages = (1..3).collect { new TestMessage("key-" + it, "value-" + it) }
            def filter = { TestMessage m -> m.key.startsWith("key-1") }

        when:
            messages.each { publish(topicName, it.asJson()) }
            5.times { publishJson(topicName) }

        then:
            1 == hermes.query().countMatchingJsonMessages(topicName, TestMessage, filter)
    }

    def "should reset received requests with Avro messages"() {
        given:
        def topicName = "my-test-avro-topic"
        hermes.define().avroTopic(topicName)
        publishAvro(topicName, new TestMessage("test-key", "test-value"))
        assert hermes.query().countAvroMessages(topicName) == 1

        when:
        hermes.resetReceivedAvroRequests(topicName, schema, TestMessage, {it -> it.key == "test-key"})

        then:
        hermes.query().countAvroMessages(topicName) == 0
    }

    def "should not reset received requests with Avro messages when predicate does not match"() {
        given:
        def topicName = "my-test-avro-topic"
        hermes.define().avroTopic(topicName)
        publishAvro(topicName, new TestMessage("test-key", "test-value"))

        when:
        hermes.resetReceivedAvroRequests(topicName, schema, TestMessage, {it -> it.key == "different-test-key"})

        then:
        hermes.query().countAvroMessages(topicName) == 1
    }

    def "should reset received requests with JSON messages"() {
        given:
        def topicName = "my-test-json-topic"
        hermes.define().avroTopic(topicName)
        publish(topicName, new TestMessage("test-key", "test-value").asJson())
        hermes.query().countJsonMessages(topicName, TestMessage) == 1

        when:
        hermes.resetReceivedJsonRequests(topicName, TestMessage, {it -> it.key == "test-key"})

        then:
        hermes.query().countJsonMessages(topicName, TestMessage) == 0
    }

    def "should not reset received requests with JSON messages when predicate does not match"() {
        given:
        def topicName = "my-test-json-topic"
        hermes.define().avroTopic(topicName)
        publish(topicName, new TestMessage("test-key", "test-value").asJson())

        when:
        hermes.resetReceivedJsonRequests(topicName, TestMessage, {it -> it.key == "different-test-key"})

        then:
        hermes.query().countJsonMessages(topicName, TestMessage) == 1
    }

    def publishJson(String topic) {
        publish(topic, TestMessage.random().asJson())
    }

    def publish(String topic, String body) {
        publisher.publish(topic, body)
    }

    def publishAvro(String topic, TestMessage message) {
        publisher.publish(topic, asAvro(message))
    }

    def asAvro(TestMessage message) {
        return jsonAvroConverter.convertToAvro(message.asJson().bytes, schema)
    }
}
