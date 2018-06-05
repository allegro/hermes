package pl.allegro.tech.hermes.mock

import org.apache.avro.Schema
import org.apache.avro.reflect.ReflectData
import org.junit.Rule
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher
import spock.lang.Specification

class HermesMockAvroTest extends Specification {
    @Rule
    HermesMockRule hermes = new HermesMockRule(56789);

    Schema schema = ReflectData.get().getSchema(TestMessage);

    protected HermesPublisher publisher = new HermesPublisher("http://localhost:56789");

    def "should receive an Avro message"() {
        given:
        def topicName = "my-test-avro-topic"
        hermes.addAvroTopic(topicName)

        when:
        publish(topicName)

        then:
        hermes.expectSingleMessageOnTopic(topicName)

        and:
        hermes.expectSingleAvroMessageOnTopic(topicName,schema)
    }

    def "should get all messages as avro"() {
        given:
        def topicName = "get-all-avro-topic"
        hermes.addAvroTopic(topicName)

        def messages = []
        5.times {
            messages << new TestMessage("key-" + it, "value-" + it)
        }

        when:
        5.times {
            publish(topicName, messages[it].toString())
        }

        then:
        hermes.expectAvroMessagesOnTopic(5, topicName, schema)
    }

    def "should throw on incorrect Avro schema"() {
        given:
        def schema = ReflectData.get().getSchema(TestMessage);
        def topicName = "my-first-avro-failing-test-topic"
        hermes.addTopic(topicName)

        when:
        publish(topicName, "whatever")

        and:
        hermes.expectSingleAvroMessageOnTopic(topicName, schema)

        then:
        def ex = thrown(HermesMockException)
        ex.message.startsWith("Failed to convert to AVRO")
    }


    def publish(String topic) {
        publish(topic, TestMessage.random().toString())
    }

    def publish(String topic, String body) {
        publisher.publish(topic, body)
    }
}
