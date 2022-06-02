package pl.allegro.tech.hermes.management.domain.subscription.validator

import com.damnhandy.uri.template.MalformedUriTemplateException
import pl.allegro.tech.hermes.api.EndpointAddress
import spock.lang.Shared
import spock.lang.Specification

import static java.util.Collections.emptyList

class EndpointAddressFormatValidatorTest extends Specification {

    @Shared
    EndpointAddressFormatValidator endpointAddressValidator = new EndpointAddressFormatValidator(emptyList())

    def "should validate valid endpoint"() {
        given:
        EndpointAddress endpointAddress = EndpointAddress.of("http://some.endpoint.com")

        when:
        endpointAddressValidator.check(endpointAddress)

        then:
        noExceptionThrown()
    }

    def "should not validate endpoint with invalid protocol"() {
        given:
        EndpointAddress endpointAddress = EndpointAddress.of("thisisstupid://some.endpoint.com")

        when:
        endpointAddressValidator.check(endpointAddress)

        then:
        def exception = thrown(EndpointValidationException)
        exception.message == "Endpoint address has invalid format"
    }

    def "should not validate endpoint address with invalid uri"() {
        given:
        EndpointAddress endpointAddress = EndpointAddress.of("jms://{}invalid.endpoint.com")

        when:
        endpointAddressValidator.check(endpointAddress)

        then:
        thrown(StringIndexOutOfBoundsException)
    }

    def "should not validate endpoint address with invalid template uri"() {
        given:
        EndpointAddress endpointAddress = EndpointAddress.of("http://thisistemplate{\\}")

        when:
        endpointAddressValidator.check(endpointAddress)

        then:
        thrown(MalformedUriTemplateException)
    }

    def "should validate endpoint address with valid template uri"() {
        given:
        EndpointAddress endpointAddress = EndpointAddress.of("http://thisistemplate/{name}")

        when:
        endpointAddressValidator.check(endpointAddress)

        then:
        noExceptionThrown()
    }

    def "should not validate uri template with invalid hostname"() {
        given:
        EndpointAddress endpointAddress = EndpointAddress.of("http://host_with_invalid_chars/{variable}")

        when:
        endpointAddressValidator.check(endpointAddress)

        then:
        def exception = thrown(EndpointValidationException)
        exception.message == "Endpoint contains invalid chars in host name. Underscore is one of them."
    }

    def "should not validate uri with invalid hostname"() {
        given:
        EndpointAddress endpointAddress = EndpointAddress.of("http://host_with_invalid_chars.com")

        when:
        endpointAddressValidator.check(endpointAddress)

        then:
        def exception = thrown(EndpointValidationException)
        exception.message == "Endpoint contains invalid chars in host name. Underscore is one of them."
    }
}
