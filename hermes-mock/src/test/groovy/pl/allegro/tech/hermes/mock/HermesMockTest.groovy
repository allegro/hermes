package pl.allegro.tech.hermes.mock

import org.junit.Rule
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher
import spock.lang.Specification

class HermesMockTest extends Specification {
    @Rule
    HermesMockRule hermes = new HermesMockRule(56789);

    protected HermesPublisher publisher = new HermesPublisher("http://localhost:56789");

    def "should receive a message"() {
        given:
        def topicName = "my-test-topic"
        hermes.addTopic(topicName)

        when:
        publish(topicName)

        then:
        hermes.expectSingleMessageOnTopic(topicName)
    }

    def "should receive 3 messages"() {
        given:
        def topicName = "my-test-topic"
        hermes.addTopic(topicName)

        when:
        3.times {
            publish(topicName)
        }

        then:
        hermes.expectMessagesOnTopic(3, topicName)
    }

    def "should receive 2 messages + 1 delayed"() {
        given:
        def topicName = "my-test-topic-3"
        hermes.addTopic(topicName)

        when:
        2.times {
            publish(topicName)
        }

        Thread.start {
            sleep(500)
            publish(topicName)
        }

        then:
        hermes.expectMessagesOnTopic(3, topicName)
    }

    def "should receive message as class"() {
        given:
        def topicName = "my-test-topic"
        hermes.addTopic(topicName)

        when:
        publish(topicName)

        then:
        hermes.expectSingleMessageOnTopicAs(topicName, TestMessage)
    }

    def "should receive all messages as a particular class"() {
        given:
        def topicName = "my-test-topic"
        hermes.addTopic(topicName)

        when:
        3.times {
            publish(topicName)
        }
        3.times {
            publish("blag")
        }

        then:
        hermes.expectMessagesOnTopicAs(3, topicName, TestMessage)
    }

    def "should throw on more than 1 message"() {
        given:
        def topicName = "my-first-failing-test-topic"
        hermes.addTopic(topicName)

        when:
        2.times {
            publish(topicName)
        }

        and:
        hermes.expectSingleMessageOnTopic(topicName)

        then:
        def ex = thrown(HermesMockException)
    }

    def "should throw on more than 1 message of particular class"() {
        given:
        def topicName = "my-first-failing-test-topic"
        hermes.addTopic(topicName)

        when:
        2.times {
            publish(topicName)
        }

        and:
        hermes.expectSingleMessageOnTopicAs(topicName, TestMessage)

        then:
        def ex = thrown(HermesMockException)
    }

    def "should get all messages"() {
        given:
        def topicName = "get-all-test-topic"
        hermes.addTopic(topicName)

        when:
        5.times {
            publish(topicName)
        }

        then:
        def requests = hermes.getAllRequests()
        requests.size() == 5
    }

    def 'should get all messages from topic'() {
        given:
        def topicName = "get-all-test-topic"
        def topicName2 = "get-all-test-topic-2"
        hermes.addTopic(topicName)
        hermes.addTopic(topicName2)

        when:
        5.times {
            publish(topicName)
        }
        2.times {
            publish(topicName2)
        }

        then:
        def requests1 = hermes.getAllRequests(topicName)
        requests1.size() == 5

        and:
        def requests2 = hermes.getAllRequests(topicName2)
        requests2.size() == 2

        and:
        hermes.getAllRequests().size() == 7
    }

    def 'should reset received messages'() {
        given:
        def topicName = "get-all-test-topic"
        hermes.addTopic(topicName)

        when:
        5.times {
            publish(topicName)
        }

        then:
        hermes.getAllRequests(topicName).size() == 5

        and:
        hermes.resetReceivedRequest()

        then:
        hermes.getAllRequests(topicName).isEmpty()
    }

    def "should get all messages as specified class"() {
        given:
        def topicName = "get-all-test-topic"
        hermes.addTopic(topicName)

        def messages = []
        5.times {
            messages << new TestMessage("key-" + it, "value-" + it)
        }

        when:
        5.times {
            publish(topicName, messages[it].toString())
        }

        then:
        def requests = hermes.getAllMessagesAs(topicName, TestMessage)
        requests.size() == 5
        requests[0].key == "key-0"
        requests[0].value == "value-0"
    }

    def "should get last message as specified class"() {
        given:
        def topicName = "my-topic"
        hermes.addTopic(topicName)

        when:
        3.times {
            publish(topicName)
        }

        then:
        def message = hermes.getLastMessageAs(topicName, TestMessage)
        message.isPresent()
        message.get().key == "random"

        def request = hermes.getLastRequest(topicName)
        request.isPresent()
        request.get().getBodyAsString() == message.get().toString()
    }

    def publish(String topic) {
        publish(topic, TestMessage.random().toString())
    }

    def publish(String topic, String body) {
        publisher.publish(topic, body)
    }
}
