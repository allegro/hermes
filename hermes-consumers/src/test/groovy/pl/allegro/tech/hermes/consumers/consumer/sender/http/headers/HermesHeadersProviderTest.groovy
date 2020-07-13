package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers

import com.google.common.collect.ImmutableSet
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpRequestData
import spock.lang.Specification

import static java.util.Collections.emptyList
import static java.util.Collections.singleton
import static java.util.Collections.singletonMap
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestHttpRequestData.requestData
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.message
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.messageWithAdditionalHeaders
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.messageWithSchemaData
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.messageWithSubscriptionData

class HermesHeadersProviderTest extends Specification {

    def "should produce #header header with correct value"() {
        given:
        HttpHeadersProvider hermesHeadersProvider = new HermesHeadersProvider(emptyList())

        when:
        Map<String, String> headers =
                hermesHeadersProvider.getHeaders(message(), requestData()).asMap()

        then:
        headers.size() == 2
        headers.get("Hermes-Message-Id") == "123"
        headers.get("Hermes-Retry-Count") == "0"
    }

    def "should contain subscription-specific headers when message has subscription identity headers"() {
        given:
        HttpHeadersProvider hermesHeadersProvider = new HermesHeadersProvider(emptyList())

        when:
        Map<String, String> headers =
                hermesHeadersProvider.getHeaders(messageWithSubscriptionData(), requestData()).
                        asMap()

        then:
        headers.size() == 4
        headers.get("Hermes-Topic-Name") == "topic1"
        headers.get("Hermes-Subscription-Name") == "subscription1"
    }

    def "should contain schema version and schema id header when schema is present"() {
        given:
        HttpHeadersProvider hermesHeadersProvider = new HermesHeadersProvider(emptyList())

        when:
        Map<String, String> headers =
                hermesHeadersProvider.getHeaders(messageWithSchemaData(), requestData()).asMap()

        then:
        headers.size() == 4
        headers.get("Schema-Version") == "1"
        headers.get("Schema-Id") == "1"
    }

    def "should contain additional headers when they are present"() {
        given:
        HttpHeadersProvider hermesHeadersProvider = new HermesHeadersProvider(emptyList())

        when:
        Map<String, String> headers =
                hermesHeadersProvider.getHeaders(messageWithAdditionalHeaders(), requestData()).
                        asMap()

        then:
        headers.size() == 3
        headers.get("additional-header") == "v"
    }

    def "should forward headers from nested provider"() {
        given:
        HttpHeadersProvider nestedHeadersProvider = new HttpHeadersProvider() {

            @Override
            HttpRequestHeaders getHeaders(Message message, HttpRequestData requestData) {
                return new HttpRequestHeaders(singletonMap("k", "v"))
            }
        }

        HttpHeadersProvider hermesHeadersProvider = new HermesHeadersProvider(
                singleton(nestedHeadersProvider))

        when:
        Map<String, String> headers =
                hermesHeadersProvider.getHeaders(message(), requestData()).asMap()

        then:
        headers.get("k") == "v"
    }

    def "should produce headers appending headers with additional providers"() {
        given:
        def additionalProviderOne = new HttpHeadersProvider() {

            @Override
            HttpRequestHeaders getHeaders(Message message, HttpRequestData requestData) {
                return new HttpRequestHeaders(singletonMap("header-1", "header-1-value"))
            }
        }

        def additionalProviderTwo = new HttpHeadersProvider() {

            @Override
            HttpRequestHeaders getHeaders(Message message, HttpRequestData requestData) {
                return new HttpRequestHeaders(singletonMap("header-2", "header-2-value"))
            }
        }

        HttpHeadersProvider hermesHeadersProvider = new HermesHeadersProvider(
                ImmutableSet.of(additionalProviderOne, additionalProviderTwo)
        )

        when:
        Map<String, String> headers =
                hermesHeadersProvider.getHeaders(message(), requestData()).asMap()

        then:
        headers.get("header-1") == "header-1-value"
        headers.get("header-2") == "header-2-value"
    }

}
