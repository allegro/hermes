package pl.allegro.tech.hermes.mock

import com.github.tomakehurst.wiremock.client.VerificationException
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
        hermes.start()

        when:
        publish(topicName)

        then:
        hermes.assertTopic(topicName)
    }

    def "should receive 3 messages"() {
        given:
        def topicName = "my-test-topic-3"
        hermes.start()

        when:
        1.upto(3, {
            publish(topicName)
        })

        then:
        hermes.expectTopic(3, topicName)
    }

    def "should throw on more than 1 message"() {
        given:
        def topicName = "my-first-failing-test-topic"
        hermes.start()

        when:
        publish(topicName)
        publish(topicName)

        and:
        hermes.assertTopic(topicName)

        then:
        def e = thrown(VerificationException)
    }

    def "should get all messages"() {
        given:
        def topicName = "get-all-test-topic"

        when:
        1.upto(5, {
            publish(topicName)
        })

        then:
        def requests = hermes.getAllRequests(topicName)
        requests.size() == 5
    }

    def setup() { hermes.start() }

    def cleanup() { hermes.stop() }

    def publish(String topic) {
        publisher.publish(topic, TestMessage.random().body())
    }
}
