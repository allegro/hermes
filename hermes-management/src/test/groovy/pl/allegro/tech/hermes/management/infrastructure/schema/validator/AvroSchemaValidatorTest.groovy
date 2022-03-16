package pl.allegro.tech.hermes.management.infrastructure.schema.validator

import groovy.json.JsonBuilder
import org.apache.commons.io.IOUtils
import spock.lang.Specification

class AvroSchemaValidatorTest extends Specification {

    private AvroSchemaValidator avroSchemaValidator = new AvroSchemaValidator(true);

    def "should accept valid schema"() {
        given:
        def schema = readSchema("/schema/user.avsc")

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

    def "should throw exception in case of empty schema"() {
        when:
        avroSchemaValidator.check("")

        then:
        def e = thrown IllegalArgumentException
        e.message == "Message schema cannot be empty"
    }

    def "should throw exception in case of lack of __metadata field"() {
        given:
        def schema = readSchema("/schema/user_no_metadata.avsc")

        when:
        avroSchemaValidator.check(schema)

        then:
        def e = thrown InvalidSchemaException
        e.message == "Error while trying to validate schema: Missing Hermes __metadata field"
    }

    def "should not require __metadata field when metadataFieldIsRequired is turned off"() {
        given:
        def schema = readSchema("/schema/user_no_metadata.avsc")
        AvroSchemaValidator validatorWithMetadataFieldNotRequired = new AvroSchemaValidator(false)

        expect:
        validatorWithMetadataFieldNotRequired.check(schema)
    }

    def "should throw exception in case of incorrect types used in __metadata field"() {
        given:
        def schema = readSchema("/schema/user_metadata_with_avro_java_string.avsc")

        when:
        avroSchemaValidator.check(schema)

        then:
        def e = thrown InvalidSchemaException
        e.message == "Error while trying to validate schema: Invalid types used in field __metadata"
    }

    private String readSchema(String path) {
        IOUtils.toString(getClass().getResourceAsStream(path), "UTF-8")
    }
}
