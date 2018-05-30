package pl.allegro.tech.hermes.mock

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.ClassRule
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher
import pl.allegro.tech.hermes.test.helper.message.TestMessage
import spock.lang.Shared
import spock.lang.Specification

class HermesMockTest extends Specification {
    @ClassRule
    @Shared
    HermesMock hermes = new HermesMock(56789);

    protected HermesPublisher publisher = new HermesPublisher("http://localhost:56789");

    def "should receive a message"() {
        given:
        def topicName = "my-test-topic"

        when:
        publish(topicName)

        then:
        hermes.assertTopic(topicName)
    }

    def "should receive 3 messages"() {
        given:
        def topicName = "my-test-topic-3"

        when:
        3.times {
            publish(topicName)
        }

        then:
        hermes.expectTopic(3, topicName)
    }

//    def "should receive 2 messages + 1 delayed"() {
//        given:
//        def topicName = "my-test-topic-3"
//
//        when:
//        2.times {
//            publish(topicName)
//        }
//
//        new Thread(new Runnable() {
//            void run() {
//                sleep(1000)
//                publish(topicName)
//            }
//        }).start();
//
//        then:
//        hermes.expectTopic(3, topicName)
//    }

    def "should throw on more than 1 message"() {
        given:
        def topicName = "my-first-failing-test-topic"

        when:
        2.times {
            publish(topicName)
        }

        and:
        hermes.assertTopic(topicName)

        then:
        def e = thrown(HermesMockException)
    }

    def "should get all messages"() {
        given:
        def topicName = "get-all-test-topic"

        when:
        5.times {
            publish(topicName)
        }

        then:
        def requests = hermes.getAllRequests(topicName)
        requests.size() == 5
    }

//    def "should get all messages as specified class"() {
//        given:
//        def topicName = "get-all-test-topic"
//        def messages = []
//        5.times {
//            messages << new TestMessage().append("key-" + it, "value-" + it)
//        }
//
//        when:
//        5.times {
//            publish(topicName, messages[it].toString())
//        }
//
//        then:
//        def requests = hermes.getAllMessagesAs(topicName, TestMessage.class)
//        requests.size() == 5
//        requests[0].getContent() != null
//        requests[0].getContent().get("key-0") == "key-0"
//    }

    def publish(String topic) {
        publisher.publish(topic, TestMessage.random().body())
    }

    def publish(String topic, String body) {
        publisher.publish(topic, body)
    }
}
