package pl.allegro.tech.hermes.schema

import pl.allegro.tech.hermes.api.SchemaSource
import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class DirectCompiledSchemaRepositoryTest extends Specification {

    static final SchemaVersion v1 = SchemaVersion.valueOf(1)

    def schemaSourceClient = Stub(SchemaSourceClient)
    def schemaCompiler = { it.value().toUpperCase() }
    def repository = new DirectCompiledSchemaRepository(schemaSourceClient, schemaCompiler)

    def topic = topic("group", "topic").build()

    def "should provide schema from source of given version"() {
        given:
        schemaSourceClient.getSchemaSource(topic.getName(), v1) >> Optional.of(SchemaSource.valueOf('stuff'))

        expect:
        repository.getSchema(topic, v1) == new CompiledSchema('STUFF', v1)
    }

    def "should fail to provide schema if schema source is missing"() {
        given:
        schemaSourceClient.getSchemaSource(topic.getName(), v1) >> Optional.empty()

        when:
        repository.getSchema(topic, v1)

        then:
        thrown CouldNotLoadSchemaException
    }

    def "should fail to provide schema if loading schema source failed"() {
        given:
        schemaSourceClient.getSchemaSource(topic.getName(), v1) >> { throw new RuntimeException("loading failed") }

        when:
        repository.getSchema(topic, v1)

        then:
        thrown CouldNotLoadSchemaException
    }

    def "should fail to provide schema if schema compilation failed"() {
        given:
        schemaSourceClient.getSchemaSource(topic.getName(), v1) >> Optional.of(SchemaSource.valueOf('stuff'))
        def repository = new DirectCompiledSchemaRepository(schemaSourceClient, {
            throw new RuntimeException("compilation failed")
        })

        when:
        repository.getSchema(topic, v1)

        then:
        thrown CouldNotLoadSchemaException
    }
}
