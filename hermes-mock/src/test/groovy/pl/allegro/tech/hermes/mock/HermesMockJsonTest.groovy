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
import static pl.allegro.tech.hermes.mock.exchange.Response.Builder.aResponse

class HermesMockJsonTest extends Specification {

    @Shared
    int port = Ports.nextAvailable()

    @ClassRule
    @Shared
    HermesMockRule hermes = new HermesMockRule(port)

    HermesPublisher publisher = new HermesPublisher("http://localhost:$port")

    def setup() {
        hermes.resetReceivedRequest()
        hermes.resetMappings()
    }

    def "should receive a Json message matched by pattern"() {
        given: "define wiremock response for matching json pattern"
            def topicName = "my-test-json-topic"
            hermes.define().jsonTopic(topicName,
                    aResponse().withStatusCode(201).build(),
                    TestMessage,
                    { it -> it.key == "test-key-pattern" })

        when: "message with matching pattern is published on topic"
            def message = new TestMessage("test-key-pattern", "test-key-value")
            def response = publishJson(topicName, message.asJson())

        then: "check for any single message on the topic and check for correct response"
            hermes.expect().singleMessageOnTopic(topicName)
            response.status == HttpStatus.SC_CREATED
    }

    def "should not match json pattern"() {
        given: "define wiremock response for matching json pattern"
            def topicName = "my-test-json-topic"
            hermes.define().jsonTopic(topicName,
                    aResponse().withStatusCode(201).build(),
                    TestMessage,
                    { it -> it.key == "non-existing-key" })

        when: "message with non-matching pattern is published on topic"
            def message = new TestMessage("test-key-pattern", "test-key-value")
            def response = publishJson(topicName, message.asJson())

        then: "check for correct response status"
            response.status == HttpStatus.SC_NOT_FOUND
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

    def "should reset received requests with JSON messages"() {
        given:
        def topicName = "my-test-json-topic"
        hermes.define().avroTopic(topicName)
        publishJson(topicName, new TestMessage("test-key", "test-value").asJson())

        when:
        hermes.query().resetReceivedJsonRequests(topicName, TestMessage, {it -> it.key == "test-key"})

        then:
        hermes.query().countAvroMessages(topicName) == 0
    }

    def "should not reset received requests with JSON messages when predicate does not match"() {
        given:
        def topicName = "my-test-json-topic"
        hermes.define().avroTopic(topicName)
        publishJson(topicName, new TestMessage("test-key", "test-value").asJson())
        def requestCountBeforeRest = hermes.query().countAvroMessages(topicName)

        when:
        hermes.query().resetReceivedJsonRequests(topicName, TestMessage, {it -> it.key == "different-test-key"})

        then:
        hermes.query().countAvroMessages(topicName) == requestCountBeforeRest
    }

    private def publishJson(String topic, String message) {
        publisher.publish(topic, message)
    }
}
