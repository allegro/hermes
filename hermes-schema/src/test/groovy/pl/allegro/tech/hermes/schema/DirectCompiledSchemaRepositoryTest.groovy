package pl.allegro.tech.hermes.schema


import pl.allegro.tech.hermes.api.RawSchemaWithMetadata
import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class DirectCompiledSchemaRepositoryTest extends Specification {

    static final SchemaVersion v1 = SchemaVersion.valueOf(1)
    static final SchemaId id1 = SchemaId.valueOf(13)

    def rawSchemaClient = Stub(RawSchemaClient)
    def schemaCompiler = {
        it.value().toUpperCase()
    }
    def repository = new DirectCompiledSchemaRepository(rawSchemaClient, schemaCompiler)

    def topic = topic("group", "topic").build()

    def "should provide schema from source of given version"() {
        given:
        rawSchemaClient.getRawSchemaWithMetadata(topic.getName(), v1) >> Optional.of(RawSchemaWithMetadata.of("stuff", id1.value(), v1.value()))

        expect:
        repository.getSchema(topic, v1) == new CompiledSchema('STUFF', id1, v1)
    }

    def "should fail to provide schema if schema source is missing"() {
        given:
        rawSchemaClient.getRawSchemaWithMetadata(topic.getName(), v1) >> Optional.empty()

        when:
        repository.getSchema(topic, v1)

        then:
        thrown SchemaNotFoundException
    }

    def "should fail to provide schema if loading schema source failed"() {
        given:
        rawSchemaClient.getRawSchemaWithMetadata(topic.getName(), v1) >> {
            throw new InternalSchemaRepositoryException(topic.qualifiedName, 500, "Unexpected failure")
        }

        when:
        repository.getSchema(topic, v1)

        then:
        def e = thrown InternalSchemaRepositoryException
        e.message.contains "500 Unexpected failure"
    }

    def "should fail to provide schema if schema compilation failed"() {
        given:
        rawSchemaClient.getRawSchemaWithMetadata(topic.getName(), v1) >> Optional.of(RawSchemaWithMetadata.of("stuff", id1.value(), v1.value()))
        def repository = new DirectCompiledSchemaRepository(rawSchemaClient, {
            throw new RuntimeException("compilation failed")
        })

        when:
        repository.getSchema(topic, v1)

        then:
        thrown RuntimeException
    }
}
