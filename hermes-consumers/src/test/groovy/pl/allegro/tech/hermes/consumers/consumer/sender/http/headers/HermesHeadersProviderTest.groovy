package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers

import pl.allegro.tech.hermes.consumers.consumer.Message
import spock.lang.Specification
import spock.lang.Unroll

import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.message
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.messageWithAdditionalHeaders
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.messageWithSchemaVersion
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.messageWithSubscriptionData

class HermesHeadersProviderTest extends Specification {

    @Unroll
    def "should produce #header header with correct value"() {
        given:
        HttpHeadersProvider hermesHeadersProvider = new HermesHeadersProvider(null)

        when:
        Map<String, String> headers = hermesHeadersProvider.getHeaders(message()).asMap()

        then:
        headers.size() == 2
        headers.get("Hermes-Message-Id") == "123"
        headers.get("Hermes-Retry-Count") == "0"
    }

    def "should contain subscription-specific headers when message has subscription identity headers"() {
        given:
        HttpHeadersProvider hermesHeadersProvider = new HermesHeadersProvider(null)

        when:
        Map<String, String> headers = hermesHeadersProvider.getHeaders(messageWithSubscriptionData()).asMap()

        then:
        headers.size() == 4
        headers.get("Hermes-Topic-Name") == "topic1"
        headers.get("Hermes-Subscription-Name") == "subscription1"
    }

    def "should contain schema version header when schema is present"() {
        given:
        HttpHeadersProvider hermesHeadersProvider = new HermesHeadersProvider(null)

        when:
        Map<String, String> headers = hermesHeadersProvider.getHeaders(messageWithSchemaVersion()).asMap()

        then:
        headers.size() == 3
        headers.get("Schema-Version") == "1"
    }

    def "should contain additional headers when they are present"() {
        given:
        HttpHeadersProvider hermesHeadersProvider = new HermesHeadersProvider(null)

        when:
        Map<String, String> headers = hermesHeadersProvider.getHeaders(messageWithAdditionalHeaders()).asMap()

        then:
        headers.size() == 3
        headers.get("additional-header") == "v"
    }

    def "should forward headers from nested provider"() {
        given:
        HttpHeadersProvider nestedHeadersProvider = new HttpHeadersProvider() {
            @Override
            HttpRequestHeaders getHeaders(Message message) {
                return new HttpRequestHeaders(Collections.singletonMap("k", "v"))
            }
        }

        HttpHeadersProvider hermesHeadersProvider = new HermesHeadersProvider(nestedHeadersProvider)

        when:
        Map<String, String> headers = hermesHeadersProvider.getHeaders(message()).asMap()

        then:
        headers.get("k") == "v"
    }

}
