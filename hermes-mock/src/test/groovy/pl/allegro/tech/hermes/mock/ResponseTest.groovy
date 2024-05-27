package pl.allegro.tech.hermes.mock

import pl.allegro.tech.hermes.mock.exchange.Response
import spock.lang.Specification
import org.apache.hc.core5.http.HttpStatus

import static pl.allegro.tech.hermes.mock.exchange.Response.Builder.aResponse

class ResponseTest extends Specification {

    def "should use status code 201 as a default"() {
        when:
        Response response = aResponse().build()

        then:
        response.statusCode == HttpStatus.SC_CREATED
    }
}
