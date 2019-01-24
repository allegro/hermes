package pl.allegro.tech.hermes.mock

import org.junit.ClassRule
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher
import spock.lang.Shared
import spock.lang.Specification

class HermesMockTest extends Specification {
    @ClassRule
    @Shared
    HermesMockRule hermes = new HermesMockRule(56789)

    HermesPublisher publisher = new HermesPublisher("http://localhost:56789")

    def setup() {
        hermes.resetReceivedRequest()
    }

    def "should receive a message"() {
        given:
            def topicName = "my-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            publish(topicName)

        then:
            hermes.expect().singleMessageOnTopic(topicName)
    }

    def "should receive 3 messages"() {
        given:
            def topicName = "my-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            3.times { publish(topicName) }

        then:
            hermes.expect().messagesOnTopic(topicName, 3)
    }

    def "should receive 2 messages + 1 delayed"() {
        given:
            def topicName = "my-test-topic-3"
            hermes.define().jsonTopic(topicName)

        when:
            2.times { publish(topicName) }

            Thread.start {
                sleep(100)
                publish(topicName)
            }

        then:
            hermes.expect().messagesOnTopic(topicName, 3)
    }

    def "should receive message as class"() {
        given:
            def topicName = "my-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            publish(topicName)

        then:
            hermes.expect().singleJsonMessageOnTopicAs(topicName, TestMessage)
    }

    def "should receive all messages as a particular class"() {
        given:
            def topicName = "my-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            3.times { publish(topicName) }
            3.times { publish("whatever") }

        then:
            hermes.expect().jsonMessagesOnTopicAs(topicName, 3, TestMessage)
    }

    def "should throw on more than 1 message"() {
        given:
            def topicName = "my-first-failing-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            2.times { publish(topicName) }

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
            2.times { publish(topicName) }

        and:
            hermes.expect().singleJsonMessageOnTopicAs(topicName, TestMessage)

        then:
            def ex = thrown(HermesMockException)
            ex.message == "Hermes mock did not receive 1 messages."
    }

    def "should get all messages"() {
        given:
            def topicName = "get-all-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            5.times { publish(topicName) }

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
            5.times { publish(topicName) }
            2.times { publish(topicName2) }

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
            5.times { publish(topicName) }

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

    def publish(String topic) {
        publish(topic, TestMessage.random().asJson())
    }

    def publish(String topic, String body) {
        publisher.publish(topic, body)
    }
}
