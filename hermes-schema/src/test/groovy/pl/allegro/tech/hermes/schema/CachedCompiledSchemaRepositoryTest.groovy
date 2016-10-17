package pl.allegro.tech.hermes.schema

import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class CachedCompiledSchemaRepositoryTest extends Specification {

    def delegate = Stub(CompiledSchemaRepository)
    def repository = new CachedCompiledSchemaRepository(delegate, 100, 100)

    def topic = topic("group", "topic").build()

    def "should provide schema from source of given version"() {
        given:
        def version = SchemaVersion.valueOf(1);
        def schema = new CompiledSchema('stuff', version)
        delegate.getSchema(topic, version) >> schema

        expect:
        repository.getSchema(topic, version) == schema
    }

    def "should provide previously compiled schema without reloading its schema source"() {
        given:
        def version = SchemaVersion.valueOf(1);
        def firstSchema = new CompiledSchema('stuff', version)
        def secondSchema = new CompiledSchema('other stuff', version)

        delegate.getSchema(topic, version) >>> [firstSchema, secondSchema]
        repository.getSchema(topic, version)

        expect:
        repository.getSchema(topic, version) == firstSchema
    }

    def "should fail to provide schema if delegate failed to load it"() {
        given:
        def version = SchemaVersion.valueOf(1);
        delegate.getSchema(topic, version) >> { throw new RuntimeException("loading failed") }

        when:
        repository.getSchema(topic, version)

        then:
        thrown CouldNotLoadSchemaException
    }

}
