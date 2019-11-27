package pl.allegro.tech.hermes.mock

import org.apache.http.HttpStatus
import org.junit.ClassRule
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Shared
import spock.lang.Specification

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

}
