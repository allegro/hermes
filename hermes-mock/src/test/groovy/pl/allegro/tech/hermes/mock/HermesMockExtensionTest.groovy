package pl.allegro.tech.hermes.mock

import org.junit.jupiter.api.extension.RegisterExtension
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Shared
import spock.lang.Specification

class HermesMockExtensionTest extends Specification {

    @Shared
    int port = Ports.nextAvailable()

    @RegisterExtension
    HermesMockExtension hermes = new HermesMockExtension(port)

    HermesPublisher publisher = new HermesPublisher("http://localhost:$port")

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
