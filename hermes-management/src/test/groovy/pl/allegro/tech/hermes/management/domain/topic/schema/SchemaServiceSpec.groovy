package pl.allegro.tech.hermes.management.domain.topic.schema

import pl.allegro.tech.hermes.schema.RawSchemaClient
import pl.allegro.tech.hermes.schema.SchemaVersion
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidatorProvider
import spock.lang.Specification

class SchemaServiceSpec extends Specification {

    RawSchemaClient rawSchemaClient = Mock()
    SchemaService schemaService = new SchemaService(rawSchemaClient, Stub(SchemaValidatorProvider), false)

    def "should return integer schema versions"() {
        given:
        rawSchemaClient.getVersions(_) >> [SchemaVersion.valueOf(1), SchemaVersion.valueOf(3)]

        expect:
        schemaService.getVersions("group.topic") == [1, 3]
    }

    def "should return empty versions when registry lookup fails"() {
        given:
        rawSchemaClient.getVersions(_) >> { throw new RuntimeException("unavailable") }

        expect:
        schemaService.getVersions("group.topic").empty
    }
}
