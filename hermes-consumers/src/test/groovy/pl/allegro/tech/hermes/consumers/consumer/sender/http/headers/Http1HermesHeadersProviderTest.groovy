package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers

import spock.lang.Specification
import spock.lang.Unroll

import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.message
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestUris.rawAddress

class Http1HermesHeadersProviderTest extends Specification {

    HttpHeadersProvider http1HeadersProvider = new Http1HeadersProvider()

    @Unroll
    def "should contain #header header with correct value for http1"() {
        expect:
        def headers = http1HeadersProvider.getHeaders(message(), rawAddress()).asMap()
        headers.get(header) == value

        where:
        header                     | value
        "Content-Type"             | "application/json"
        "Keep-Alive"               | "true"
    }

}
