package pl.allegro.tech.hermes.schema

import spock.lang.Specification

import java.time.Duration

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class CachedCompiledSchemaRepositoryTest extends Specification {

    def delegate = Stub(CompiledSchemaRepository)
    def repository = new CachedCompiledSchemaRepository(delegate, 100, Duration.ofHours(100))

    def topic = topic("group", "topic").build()

    def "should provide schema from source of given version"() {
        given:
        def id = SchemaId.valueOf(1)
        def version = SchemaVersion.valueOf(1)
        def schema = new CompiledSchema('stuff', id, version)
        delegate.getSchema(topic, version) >> schema

        expect:
        repository.getSchema(topic, version) == schema
    }

    def "should provide previously compiled schema without reloading its schema source"() {
        given:
        def id = SchemaId.valueOf(1)
        def version = SchemaVersion.valueOf(1)
        def firstSchema = new CompiledSchema('stuff', id, version)
        def secondSchema = new CompiledSchema('other stuff', id, version)

        delegate.getSchema(topic, version) >>> [firstSchema, secondSchema]
        repository.getSchema(topic, version)

        expect:
        repository.getSchema(topic, version) == firstSchema
    }

    def "should fail to provide schema if delegate failed to load it"() {
        given:
        def version = SchemaVersion.valueOf(1)
        delegate.getSchema(topic, version) >> { throw new RuntimeException("loading failed") }

        when:
        repository.getSchema(topic, version)

        then:
        thrown CouldNotLoadSchemaException
    }

    def "should remove schema from cache on topic removal"() {
        given:
        def id = SchemaId.valueOf(1)
        def version = SchemaVersion.valueOf(1)
        def firstSchema = new CompiledSchema('stuff', id, version)
        def secondSchema = new CompiledSchema('other stuff', id, version)

        delegate.getSchema(topic, version) >>> [firstSchema, secondSchema]

        expect:
        repository.getSchema(topic, version) == firstSchema

        when:
        repository.removeFromCache(topic)

        then:
        repository.getSchema(topic, version) == secondSchema
    }

}
