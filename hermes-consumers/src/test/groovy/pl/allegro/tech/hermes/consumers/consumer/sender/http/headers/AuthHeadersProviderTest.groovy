package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers

import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProvider
import spock.lang.Specification

import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.message

class AuthHeadersProviderTest extends Specification {

    HttpAuthorizationProvider authorizationProvider = { Optional.of("token") }

    def "should produce authorization header when authorization provider is present"() {
        given:
        HttpHeadersProvider authHeadersProvider = new AuthHeadersProvider(null, authorizationProvider)

        when:
        Map<String, String> headers = authHeadersProvider.getHeaders(message()).asMap()

        then:
        headers.size() == 1
        headers.get("Authorization", "token")
    }

    def "should not produce authorization header when authorization provider is not present"() {
        given:
        HttpHeadersProvider authHeadersProvider = new AuthHeadersProvider(null, null)

        when:
        Map<String, String> headers = authHeadersProvider.getHeaders(message()).asMap()

        then:
        headers.size() == 0
    }

    def "should forward headers from nested provider"() {
        given:
        HttpHeadersProvider nestedHeadersProvider = new HttpHeadersProvider() {
            @Override
            HttpRequestHeaders getHeaders(Message message) {
                return new HttpRequestHeaders(Collections.singletonMap("k", "v"))
            }
        }

        HttpHeadersProvider authHeadersProvider = new AuthHeadersProvider(nestedHeadersProvider, null)

        when:
        Map<String, String> headers = authHeadersProvider.getHeaders(message()).asMap()

        then:
        headers.size() == 1
        headers.get("k") == "v"
    }

}
