package pl.allegro.tech.hermes.api

import spock.lang.Specification

class EndpointAddressTest extends Specification {

    def "should return decomposed parts of URI when matching URI pattern"() {
        when:
        EndpointAddress endpoint = EndpointAddress.of('http://user:password@endpoint.com')

        then:
        endpoint.endpoint == 'http://endpoint.com'
        endpoint.username == 'user'
        endpoint.password == 'password'
        endpoint.protocol == 'http'
        endpoint.containsCredentials()
    }

    def "should return decomposed parts of URI when matching URI pattern containing https scheme"() {
        when:
        EndpointAddress endpoint = EndpointAddress.of('https://user:password@endpoint.com')

        then:
        endpoint.endpoint == 'https://endpoint.com'
        endpoint.username == 'user'
        endpoint.password == 'password'
        endpoint.protocol == 'https'
        endpoint.containsCredentials()
    }

    def "should return decomposed parts of URI when matching URI pattern with username containing underscores ,dot and dash"() {
        when:
        EndpointAddress endpoint = EndpointAddress.of('https://user-_.:password@endpoint.com')

        then:
        endpoint.endpoint == 'https://endpoint.com'
        endpoint.username == 'user-_.'
        endpoint.password == 'password'
        endpoint.protocol == 'https'
        endpoint.containsCredentials()
    }

    def "should not mark URI as containing credentials when there are none specified"() {
        when:
        EndpointAddress endpoint = EndpointAddress.of('http://endpoint.com')

        then:
        endpoint.endpoint == 'http://endpoint.com'
        endpoint.username == null
        endpoint.password == null
        endpoint.protocol == 'http'
        !endpoint.containsCredentials()
    }

    def "should not decompose to URI parts when wierd syntax is used"() {
        when:
        EndpointAddress endpoint = EndpointAddress.of('this is wierd')

        then:
        endpoint.endpoint == 'this is wierd'
        endpoint.username == null
        endpoint.password == null
        endpoint.protocol == null
    }

    def "should anonymize password"() {
        when:
        EndpointAddress anonymizedEndpoint = EndpointAddress.of('http://user:password@endpoint.com').anonymize()

        then:
        anonymizedEndpoint.password == '*****'
    }

    def "should append anonymized password to rawEndpoint"() {
        when:
        EndpointAddress anonymizedEndpoint = EndpointAddress.of('http://user-_:password@endpoint.com').anonymize()

        then:
        anonymizedEndpoint.rawEndpoint == 'http://user-_:*****@endpoint.com'
    }
}
