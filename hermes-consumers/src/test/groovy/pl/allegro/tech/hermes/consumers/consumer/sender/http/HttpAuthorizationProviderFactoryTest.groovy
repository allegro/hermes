package pl.allegro.tech.hermes.consumers.consumer.sender.http

import pl.allegro.tech.hermes.api.EndpointAddress
import spock.lang.Specification

class HttpAuthorizationProviderFactoryTest extends Specification {

    private final HttpAuthorizationProviderFactory factory = new HttpAuthorizationProviderFactory()

    def "should return empty when URL has no credentials"() {
        given:
        EndpointAddress address = EndpointAddress.of("http://example.com")

        expect:
        !factory.create(address).present
    }

    def "should return BasicAuth provider when URL contains credentials"() {
        given:
        EndpointAddress address = EndpointAddress.of("http://user:password@example.com")

        expect:
        factory.create(address).get() instanceof BasicAuthProvider
    }

}
