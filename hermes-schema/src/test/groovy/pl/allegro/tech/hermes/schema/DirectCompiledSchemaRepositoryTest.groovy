package pl.allegro.tech.hermes.schema

import pl.allegro.tech.hermes.api.RawSchema
import spock.lang.Specification

import javax.ws.rs.core.Response

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class DirectCompiledSchemaRepositoryTest extends Specification {

    static final SchemaVersion v1 = SchemaVersion.valueOf(1)

    def rawSchemaClient = Stub(RawSchemaClient)
    def schemaCompiler = { it.value().toUpperCase() }
    def repository = new DirectCompiledSchemaRepository(rawSchemaClient, schemaCompiler)

    def topic = topic("group", "topic").build()

    def "should provide schema from source of given version"() {
        given:
        rawSchemaClient.getSchema(topic.getName(), v1) >> Optional.of(RawSchema.valueOf('stuff'))

        expect:
        repository.getSchema(topic, v1) == new CompiledSchema('STUFF', v1)
    }

    def "should fail to provide schema if schema source is missing"() {
        given:
        rawSchemaClient.getSchema(topic.getName(), v1) >> Optional.empty()

        when:
        repository.getSchema(topic, v1)

        then:
        thrown SchemaNotFoundException
    }

    def "should fail to provide schema if loading schema source failed"() {
        given:
        rawSchemaClient.getSchema(topic.getName(), v1) >> {
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
        rawSchemaClient.getSchema(topic.getName(), v1) >> Optional.of(RawSchema.valueOf('stuff'))
        def repository = new DirectCompiledSchemaRepository(rawSchemaClient, {
            throw new RuntimeException("compilation failed")
        })

        when:
        repository.getSchema(topic, v1)

        then:
        thrown RuntimeException
    }
}
