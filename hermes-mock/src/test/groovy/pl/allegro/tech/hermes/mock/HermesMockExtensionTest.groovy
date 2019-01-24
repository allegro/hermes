package pl.allegro.tech.hermes.mock

import org.junit.jupiter.api.extension.RegisterExtension
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher
import spock.lang.Specification

class HermesMockExtensionTest extends Specification {

    @RegisterExtension
    HermesMockExtension hermes = new HermesMockExtension(56789)

    HermesPublisher publisher = new HermesPublisher("http://localhost:56789")

    def 'simple publish-expect test'() {
        given:
            def topicName = "my-test-topic"
            hermes.define().jsonTopic(topicName)

        when:
            publish(topicName, TestMessage.random().asJson())

        then:
            hermes.expect().singleMessageOnTopic(topicName)
    }

    def publish(String topic, String body) {
        publisher.publish(topic, body)
    }
}
