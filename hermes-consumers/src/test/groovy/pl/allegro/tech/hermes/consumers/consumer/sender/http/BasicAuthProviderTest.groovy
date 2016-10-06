package pl.allegro.tech.hermes.consumers.consumer.sender.http

import pl.allegro.tech.hermes.api.EndpointAddress
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.BasicAuthProvider
import spock.lang.Specification

class BasicAuthProviderTest extends Specification {

    def "should create contents of Authorization header for Basic Auth based on username and password"() {
        given:
        EndpointAddress address = EndpointAddress.of("http://user:password@example.com")
        BasicAuthProvider provider = new BasicAuthProvider(address)

        when:
        String header = provider.authorizationToken().get()

        then:
        header == 'Basic dXNlcjpwYXNzd29yZA=='
    }
}
