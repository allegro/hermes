package pl.allegro.tech.hermes.management.domain.topic

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonBuilder
import spock.lang.Specification

class SchemaValidatorTest extends Specification {

    private SchemaValidator schemaValidator = new SchemaValidator(new ObjectMapper());

    def "should accept valid schema"() {
        given:
        def json = new JsonBuilder()
        json {
            type "boolean"
        }

        expect:
        schemaValidator.check(json.toString())
    }

    def "should throw exception for invalid schema"() {
        given:
        def json = new JsonBuilder()
        json {
            type "error"
        }

        when:
        schemaValidator.check(json.toString())

        then:
        thrown(InvalidSchemaException)
    }


    def "should throw exception for invalid json"() {
        when:
        schemaValidator.check('{')

        then:
        thrown(InvalidSchemaException)
    }
}
