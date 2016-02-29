package pl.allegro.tech.hermes.domain.topic.schema

import pl.allegro.tech.hermes.api.SchemaSource
import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class CachedConcreteSchemaRepositoryTest extends Specification {

    def schemaSourceProvider = Stub(SchemaSourceProvider)
    def schemaCompiler = { it.value().toUpperCase() }
    def repository = new CachedConcreteSchemaRepository(schemaSourceProvider, 100, schemaCompiler)

    def topic = topic("group", "topic").build()

    def "should provide schema from source of given version"() {
        given:
        schemaSourceProvider.get(topic, 1) >> Optional.of(SchemaSource.valueOf('stuff'))

        expect:
        repository.getSchema(topic, 1) == new VersionedSchema('STUFF', 1)
    }

    def "should provide previously compiled schema without reloading its schema source"() {
        given:
        schemaSourceProvider.get(topic, 1) >>> [Optional.of(SchemaSource.valueOf('stuff')), Optional.of(SchemaSource.valueOf('other stuff'))]
        repository.getSchema(topic, 1)

        expect:
        repository.getSchema(topic, 1) == new VersionedSchema('STUFF', 1)
    }

    def "should fail to provide schema if schema source is missing"() {
        given:
        schemaSourceProvider.get(topic, 1) >> Optional.empty()

        when:
        repository.getSchema(topic, 1)

        then:
        thrown(CouldNotLoadSchemaException)
    }

    def "should fail to provide schema if loading schema source failed"() {
        given:
        schemaSourceProvider.get(topic, 1) >> { throw new RuntimeException("loading failed") }

        when:
        repository.getSchema(topic, 1)

        then:
        thrown(CouldNotLoadSchemaException)
    }

    def "should fail to provide schema if schema compilation failed"() {
        given:
        schemaSourceProvider.get(topic, 1) >> Optional.of(SchemaSource.valueOf('stuff'))
        def repository = new CachedConcreteSchemaRepository(schemaSourceProvider, 100, { throw new RuntimeException("compilation failed")})

        when:
        repository.getSchema(topic, 1)

        then:
        thrown(CouldNotLoadSchemaException)
    }

}
