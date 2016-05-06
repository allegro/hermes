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
        EndpointAddress anonymizedEndpoint = EndpointAddress.of('http://user:password@endpoint.com').anonymizePassword()

        then:
        anonymizedEndpoint.password == '*****'
    }

    def "should append anonymized password to rawEndpoint"() {
        when:
        EndpointAddress anonymizedEndpoint = EndpointAddress.of('http://user:password@endpoint.com').anonymizePassword()

        then:
        anonymizedEndpoint.rawEndpoint == 'http://user:*****@endpoint.com'
    }
}
