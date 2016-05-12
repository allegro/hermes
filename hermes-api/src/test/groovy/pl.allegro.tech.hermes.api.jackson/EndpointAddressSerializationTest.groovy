package pl.allegro.tech.hermes.api.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import pl.allegro.tech.hermes.api.EndpointAddress
import spock.lang.Specification

class EndpointAddressSerializationTest extends Specification {

    private final ObjectMapper mapper = new ObjectMapper()

    def "should deserialize string to endpoint address"() {
        given:
        String json = '"http://example.com"'

        when:
        EndpointAddress endpoint = mapper.readValue(json.getBytes('UTF-8'), EndpointAddress.class)

        then:
        endpoint.endpoint == 'http://example.com'
    }

    def "should serialize endpoint address to string"() {
        when:
        String json = mapper.writeValueAsString(EndpointAddress.of('http://example.com'))

        then:
        json == '"http://example.com"'
    }
}
