package pl.allegro.tech.hermes.mock

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.avro.Schema
import org.apache.avro.reflect.ReflectData
import org.junit.Rule
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher
import spock.lang.Specification
import tech.allegro.schema.json2avro.converter.JsonAvroConverter

class HermesMockAvroTest extends Specification {
    @Rule
    HermesMockRule hermes = new HermesMockRule(56789);

    Schema schema = ReflectData.get().getSchema(TestMessage);

    private HermesPublisher publisher = new HermesPublisher("http://localhost:56789");

    def "should receive an Avro message"() {
        given:
        def topicName = "my-test-avro-topic"
        hermes.define().avroTopic(topicName)

        when:
        publish(topicName)

        then:
        hermes.expect().singleMessageOnTopic(topicName)

        and:
        hermes.expect().singleAvroMessageOnTopic(topicName, schema)
    }

    def "should get all messages as avro"() {
        given:
        def topicName = "get-all-avro-topic"
        hermes.define().avroTopic(topicName)

        def messages = []
        5.times {
            messages << new TestMessage("key-" + it, "value-" + it)
        }

        when:
        5.times {
            publish(topicName, messages[it].toString())
        }

        then:
        hermes.expect().avroMessagesOnTopic(5, topicName, schema)
    }

    def "should schema from file"() {
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
        def obj = new ObjectMapper().readValue(json, HashMap.class);
        def avro = new JsonAvroConverter().convertToAvro(json.bytes, schema)

        when:
        publish(topicName, avro)

        then:
        def received = hermes.query().lastAvroMessageAs(topicName, schema, HashMap.class).get()
        obj == received
    }

    def "should throw on incorrect Avro schema"() {
        given:
        def schema = ReflectData.get().getSchema(TestMessage);
        def topicName = "my-first-avro-failing-test-topic"
        hermes.define().avroTopic(topicName)

        when:
        publish(topicName, "whatever")

        and:
        hermes.query().lastAvroMessageAs(topicName, schema, HashMap.class)

        then:
        def ex = thrown(HermesMockException)
        ex.message.startsWith("Cannot convert body")
    }

    def publish(String topic, String body) {
        publisher.publish(topic, body)
    }

    def publish(String topic, byte[] body) {
        publisher.publish(topic, body)
    }
}
