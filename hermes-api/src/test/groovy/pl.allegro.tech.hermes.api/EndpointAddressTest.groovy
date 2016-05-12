package pl.allegro.tech.hermes.api

import spock.lang.Specification

class EndpointAddressTest extends Specification {

    def "should return decomposed parts of URI when matching URI pattern"() {
        when:
        EndpointAddress endpoint = EndpointAddress.of('http://endpoint.com')

        then:
        endpoint.endpoint == 'http://endpoint.com'
        endpoint.protocol == 'http'
    }

    def "should not decompose to URI parts when wierd syntax is used"() {
        when:
        EndpointAddress endpoint = EndpointAddress.of('this is wierd')

        then:
        endpoint.endpoint == 'this is wierd'

        endpoint.protocol == null
    }

}
