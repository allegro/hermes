package pl.allegro.tech.hermes.mock

import org.apache.http.HttpStatus
import org.junit.ClassRule
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

import static java.time.Instant.now
import static pl.allegro.tech.hermes.mock.Response.Builder.aResponse

class HermesMockJsonTest extends Specification {

    @Shared
    int port = Ports.nextAvailable()

    @ClassRule
    @Shared
    HermesMockRule hermes = new HermesMockRule(port)

    HermesPublisher publisher = new HermesPublisher("http://localhost:$port")

    def setup() {
        hermes.resetReceivedRequest()
    }

    def "should receive an json message"() {
        given:
        def topicName = "my-test-json-topic"
        hermes.define().jsonTopic(topicName, HttpStatus.SC_OK)

        when:
        def response = publisher.publish(topicName, "Basic Request")

        then:
        hermes.expect().singleMessageOnTopic(topicName)
        response.status == HttpStatus.SC_OK
    }

    def "should respond with a delay"() {
        given:
        def topicName = "my-test-json-topic"
        Duration fixedDelay = Duration.ofMillis(500)
        hermes.define().jsonTopic(topicName, aResponse().withFixedDelay(fixedDelay).build())
        Instant start = now()

        when:
        publisher.publish(topicName, "Basic Request")

        then:
        hermes.expect().singleMessageOnTopic(topicName)
        Duration.between(start, now()) >= fixedDelay
    }

    def "should respond for a message send with delay"() {
        given:
        def topicName = "my-test-json-topic"
        def delayInMillis = 2_000
        hermes.define().jsonTopic(topicName)

        when:
        Thread.start {
            Thread.sleep(delayInMillis)
            publisher.publish(topicName, TestMessage.random().asJson())
        }

        then:
        hermes.expect().singleJsonMessageOnTopicAs(topicName, TestMessage)
    }

}
