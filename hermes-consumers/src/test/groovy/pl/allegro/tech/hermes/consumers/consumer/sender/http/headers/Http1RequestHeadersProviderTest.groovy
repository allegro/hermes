package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers

import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProvider
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.message
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.messageWithAdditionalHeaders

class Http1RequestHeadersProviderTest extends Specification {

    @Shared
    HttpAuthorizationProvider authorizationProvider = new HttpAuthorizationProvider() {
        @Override
        Optional<String> authorizationToken() {
            return Optional.of("token")
        }
    }

    @Shared
    HttpRequestHeadersProvider headersProvider = new Http1RequestHeadersProvider(Optional.empty())

    @Shared
    HttpRequestHeadersProvider headersProviderWithAuthorization = new Http1RequestHeadersProvider(Optional.of(authorizationProvider))

    @Unroll
    def "should contain #header header with correct value"() {
        expect:
        def headers = provider.getHeaders(message).asMap()
        headers.get(header) == value

        where:
        provider                         |   message                        |   header               |   value
        headersProvider                  |   message()                      |   "Content-Type"       |   "application/json"
        headersProvider                  |   message()                      |   "Keep-Alive"         |   "true"
        headersProvider                  |   message()                      |   "Hermes-Message-Id"  |   "123"
        headersProvider                  |   message()                      |   "Hermes-Retry-Count" |   "0"
        headersProvider                  |   messageWithAdditionalHeaders() |   "key"                |   "value"
        headersProviderWithAuthorization |   message()                      |   "Authorization"      |   "token"
    }

}
