package pl.allegro.tech.hermes.management.infrastructure.schema.validator

import groovy.json.JsonBuilder
import org.apache.commons.io.IOUtils
import spock.lang.Specification

class AvroSchemaValidatorTest extends Specification {

    private AvroSchemaValidator avroSchemaValidator = new AvroSchemaValidator();

    def "should accept valid schema"() {
        given:
        def schema = IOUtils.toString(getClass().getResourceAsStream("/schema/user.avsc"), "UTF-8")

        expect:
        avroSchemaValidator.check(schema)
    }

    def "should throw exception for invalid schema"() {
        given:
        def json = new JsonBuilder()
        json {
            type "error"
        }

        when:
        avroSchemaValidator.check(json.toString())

        then:
        thrown(InvalidSchemaException)
    }


    def "should throw exception for invalid json"() {
        when:
        avroSchemaValidator.check('{')

        then:
        thrown(InvalidSchemaException)
    }
}