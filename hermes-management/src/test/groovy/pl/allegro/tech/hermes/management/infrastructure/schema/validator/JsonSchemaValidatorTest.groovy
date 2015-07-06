package pl.allegro.tech.hermes.management.infrastructure.schema.validator

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonBuilder
import spock.lang.Specification

class JsonSchemaValidatorTest extends Specification {

    private JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(new ObjectMapper());

    def "should accept valid schema"() {
        given:
        def json = new JsonBuilder()
        json {
            type "boolean"
        }

        expect:
        jsonSchemaValidator.check(json.toString())
    }

    def "should throw exception for invalid schema"() {
        given:
        def json = new JsonBuilder()
        json {
            type "error"
        }

        when:
        jsonSchemaValidator.check(json.toString())

        then:
        thrown(InvalidSchemaException)
    }


    def "should throw exception for invalid json"() {
        when:
        jsonSchemaValidator.check('{')

        then:
        thrown(InvalidSchemaException)
    }
}
