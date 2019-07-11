package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers

import pl.allegro.tech.hermes.consumers.consumer.Message
import spock.lang.Specification

import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.createMessageWithSchema
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.createMessageWithSubscriptionData

class HermesHeadersProviderTest extends Specification {

    HermesHeadersProvider headersProvider = HermesHeadersProvider.INSTANCE

    def "should contain topic and subscription name headers when message has subscription identity headers"() {
        given:
        Message message = createMessageWithSubscriptionData()

        when:
        HttpRequestHeaders headers = headersProvider.getHeaders(message)

        then:
        def headersAsMap = headers.asMap()
        headersAsMap.get("Hermes-Topic-Name") == "topic1"
        headersAsMap.get("Hermes-Subscription-Name") == "subscription1"
    }

    def "should contain schema version header when message has schema"() {
        given:
        Message message = createMessageWithSchema()

        when:
        HttpRequestHeaders headers = headersProvider.getHeaders(message)

        then:
        headers.asMap().get("Schema-Version") == "1"
    }

}
