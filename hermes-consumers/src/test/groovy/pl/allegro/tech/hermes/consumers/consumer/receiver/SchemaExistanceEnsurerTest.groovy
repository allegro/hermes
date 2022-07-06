package pl.allegro.tech.hermes.consumers.consumer.receiver

import pl.allegro.tech.hermes.schema.CompiledSchema
import pl.allegro.tech.hermes.schema.CouldNotLoadSchemaException
import pl.allegro.tech.hermes.schema.SchemaNotFoundException
import pl.allegro.tech.hermes.schema.SchemaRepository
import pl.allegro.tech.hermes.schema.SchemaVersion
import pl.allegro.tech.hermes.schema.SchemaVersionDoesNotExistException
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;
import spock.lang.Specification


import java.time.Duration;

class SchemaExistenceEnsurerTest extends Specification {
    SchemaRepository schemaRepository
    SchemaExistenceEnsurer schemaEnsurer

    def setup() {
        schemaRepository = Mock(SchemaRepository)
        schemaEnsurer = new SchemaExistenceEnsurer(schemaRepository, Duration.ofMillis(100))
    }

    def "should retry pulling schema until it is successfully downloaded"() {
        given:
        def topic = TopicBuilder.topic("pl.allegro.someTestTopic").build()
        def compiledSchema = CompiledSchema.<String>of("schema", 1, 1)

        when:
        schemaEnsurer.ensureSchemaExists(topic, 1)

        then:
        4 * schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(1)) >>
                { throw new SchemaNotFoundException(topic, SchemaVersion.valueOf(1)) } >>
                { throw new SchemaVersionDoesNotExistException(topic, SchemaVersion.valueOf(1)) } >>
                { throw new CouldNotLoadSchemaException(topic, SchemaVersion.valueOf(1), new RuntimeException()) } >>
                compiledSchema
        3 * schemaRepository.getVersions(topic, true)
    }

    def "should not pull online schema if it's already in cache"() {
        given:
        def topic = TopicBuilder.topic("pl.allegro.someTestTopic").build()
        def compiledSchema = CompiledSchema.<String>of("schema", 1, 1)

        when:
        schemaEnsurer.ensureSchemaExists(topic, 1)

        then:
        1 * schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(1)) >> compiledSchema
        0 * schemaRepository.getVersions(topic, SchemaVersion.valueOf(1))
    }
}
