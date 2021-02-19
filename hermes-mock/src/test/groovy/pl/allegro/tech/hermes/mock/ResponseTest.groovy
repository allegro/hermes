package pl.allegro.tech.hermes.mock

import org.apache.http.HttpStatus
import spock.lang.Specification

import static pl.allegro.tech.hermes.mock.Response.Builder.aResponse

class ResponseTest extends Specification {

    def "should use status code 201 as a default"() {
        when:
        Response response = aResponse().build()

        then:
        response.statusCode == HttpStatus.SC_CREATED
    }
}
