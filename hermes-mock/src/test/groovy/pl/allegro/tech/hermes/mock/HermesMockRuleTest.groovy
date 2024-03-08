package pl.allegro.tech.hermes.mock

import org.junit.Rule
import pl.allegro.tech.hermes.test.helper.client.integration.FrontendTestClient
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Shared
import spock.lang.Specification

class HermesMockRuleTest extends Specification {

    @Shared
    int port = Ports.nextAvailable()

    @Rule
    HermesMockRule hermes = new HermesMockRule(port)

    FrontendTestClient publisher = new FrontendTestClient(port)

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
