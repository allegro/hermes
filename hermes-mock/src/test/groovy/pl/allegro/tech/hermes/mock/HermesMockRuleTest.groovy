package pl.allegro.tech.hermes.mock

import org.junit.Rule
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher
import spock.lang.Specification

class HermesMockRuleTest extends Specification {
    @Rule
    HermesMockRule hermes = new HermesMockRule(5679)

    HermesPublisher publisher = new HermesPublisher("http://localhost:5679")

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
