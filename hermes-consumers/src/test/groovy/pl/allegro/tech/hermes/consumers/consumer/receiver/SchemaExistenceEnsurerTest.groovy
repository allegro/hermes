package pl.allegro.tech.hermes.consumers.consumer.receiver

import pl.allegro.tech.hermes.common.message.wrapper.SchemaOnlineChecksRateLimiter
import pl.allegro.tech.hermes.schema.CompiledSchema
import pl.allegro.tech.hermes.schema.SchemaId
import pl.allegro.tech.hermes.schema.SchemaNotFoundException
import pl.allegro.tech.hermes.schema.SchemaRepository
import pl.allegro.tech.hermes.schema.SchemaVersion
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;
import spock.lang.Specification


class SchemaExistenceEnsurerTest extends Specification {
    SchemaRepository schemaRepository
    SchemaExistenceEnsurer schemaEnsurer
    SchemaOnlineChecksRateLimiter rateLimiter

    def setup() {
        schemaRepository = Mock(SchemaRepository)
        rateLimiter = Mock(SchemaOnlineChecksRateLimiter)
        schemaEnsurer = new SchemaExistenceEnsurer(schemaRepository, rateLimiter)
    }

    def "should check if schema exists by version"() {
        given:
        def topic = TopicBuilder.topic("pl.allegro.someTestTopic").build()
        def compiledSchema = CompiledSchema.<String> of("schema", 1, 1)
        rateLimiter.tryAcquireOnlineCheckPermit() >> true

        when:
        schemaEnsurer.ensureSchemaExists(topic, SchemaVersion.valueOf(1))

        then:
        1 * schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(1)) >> compiledSchema
    }

    def "should throw error and pull schema online by version if schema does not exist"() {
        given:
        def topic = TopicBuilder.topic("pl.allegro.someTestTopic").build()
        rateLimiter.tryAcquireOnlineCheckPermit() >> true

        when:
        schemaEnsurer.ensureSchemaExists(topic, SchemaVersion.valueOf(1))

        then:
        thrown(SchemaExistenceEnsurer.SchemaNotLoaded)
        1 * schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(1)) >> {
            throw new SchemaNotFoundException(topic, SchemaVersion.valueOf(1))
        }
        1 * schemaRepository.refreshVersions(topic)
    }

    def "should not pull online schema by version if it's already in cache"() {
        given:
        def topic = TopicBuilder.topic("pl.allegro.someTestTopic").build()
        def compiledSchema = CompiledSchema.<String> of("schema", 1, 1)
        rateLimiter.tryAcquireOnlineCheckPermit() >> true

        when:
        schemaEnsurer.ensureSchemaExists(topic, SchemaVersion.valueOf(1))

        then:
        1 * schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(1)) >> compiledSchema
        0 * schemaRepository.refreshVersions(topic)
    }

    def "should check if schema exists by id"() {
        given:
        def topic = TopicBuilder.topic("pl.allegro.someTestTopic").build()
        def compiledSchema = CompiledSchema.<String> of("schema", 1, 1)
        rateLimiter.tryAcquireOnlineCheckPermit() >> true

        when:
        schemaEnsurer.ensureSchemaExists(topic, SchemaId.valueOf(1))

        then:
        1 * schemaRepository.getAvroSchema(topic, SchemaId.valueOf(1)) >> compiledSchema
    }

    def "should throw error if schema does not exist by id"() {
        given:
        def topic = TopicBuilder.topic("pl.allegro.someTestTopic").build()
        rateLimiter.tryAcquireOnlineCheckPermit() >> true

        when:
        schemaEnsurer.ensureSchemaExists(topic, SchemaId.valueOf(1))

        then:
        thrown(SchemaExistenceEnsurer.SchemaNotLoaded)
        1 * schemaRepository.getAvroSchema(topic, SchemaId.valueOf(1)) >> {
            throw new SchemaNotFoundException(topic, SchemaVersion.valueOf(1))
        }
    }

    def "should rate limit online schema pulls by version"() {
        given:
        def topic = TopicBuilder.topic("pl.allegro.someTestTopic").build()
        rateLimiter.tryAcquireOnlineCheckPermit() >> false

        when:
        schemaEnsurer.ensureSchemaExists(topic, SchemaVersion.valueOf(1))

        then:
        thrown(SchemaExistenceEnsurer.SchemaNotLoaded)
        0 * schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(1))
    }

    def "should rate limit online schema pulls by id"() {
        given:
        def topic = TopicBuilder.topic("pl.allegro.someTestTopic").build()
        rateLimiter.tryAcquireOnlineCheckPermit() >> false

        when:
        schemaEnsurer.ensureSchemaExists(topic, SchemaId.valueOf(1))

        then:
        thrown(SchemaExistenceEnsurer.SchemaNotLoaded)
        0 * schemaRepository.getAvroSchema(topic, SchemaId.valueOf(1))
    }
}
