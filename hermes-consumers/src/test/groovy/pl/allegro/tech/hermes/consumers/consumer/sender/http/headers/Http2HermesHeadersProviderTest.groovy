package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers

import pl.allegro.tech.hermes.consumers.consumer.Message
import spock.lang.Specification
import spock.lang.Unroll

import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestMessages.message
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.TestUris.rawAddress

class Http2HermesHeadersProviderTest extends Specification {

    HttpHeadersProvider http2HeadersProvider = new Http2HeadersProvider()

    @Unroll
    def "should contain #header header with correct value for http1"() {
        when:
        def headers = http2HeadersProvider.getHeaders(message(), rawAddress()).asMap()

        then:
        headers.get( "Content-Type") == "application/json"
    }

    def "should not contain keep-alive header"() {
        given:
        Message message = message()

        when:
        HttpRequestHeaders headers = http2HeadersProvider.getHeaders(message, rawAddress())

        then:
        !headers.asMap().containsKey("Keep-Alive")
    }

}
