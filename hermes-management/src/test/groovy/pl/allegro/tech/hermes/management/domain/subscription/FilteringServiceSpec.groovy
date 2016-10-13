package pl.allegro.tech.hermes.management.domain.subscription

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.MessageFilterSpecification
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.filtering.MessageFilters
import pl.allegro.tech.hermes.common.filtering.avro.AvroPathSubscriptionMessageFilterCompiler
import pl.allegro.tech.hermes.common.filtering.json.JsonPathSubscriptionMessageFilterCompiler
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository
import pl.allegro.tech.hermes.domain.topic.schema.SchemaVersion
import pl.allegro.tech.hermes.domain.topic.schema.UnknownSchemaVersionException
import pl.allegro.tech.hermes.management.api.mappers.MessageValidationWrapper
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader
import spock.lang.Specification

class FilteringServiceSpec extends Specification {

    MessageFilters messageFilters = new MessageFilters([], [new AvroPathSubscriptionMessageFilterCompiler(), new JsonPathSubscriptionMessageFilterCompiler()])
    SchemaRepository schemaRepository = Mock()
    Topic topic = Mock()

    def filteringService = new FilteringService(messageFilters, schemaRepository)
    def spec1 = new MessageFilterSpecification([path: ".id", matcher: "0001"])
    def spec2 = new MessageFilterSpecification([path: ".name", matcher: "abc"])

    def schema = AvroUserSchemaLoader.load("/simple.avsc")

    def "should filter json message"() {
        given:
        def json = '''
        {
            "id": "0001",
            "name": "XYZ"
        }
        '''
        def wrapper = new MessageValidationWrapper(json, [spec1, spec2], null)
        topic.getContentType() >> ContentType.JSON

        when:
        def result = filteringService.isFiltered(wrapper, topic)

        then:
        result.filtered
        0 * schemaRepository._
    }

    def "should not filter json message"() {
        given:
        def json = '''
        {
            "id": "0001",
            "name": "abc"
        }
        '''
        def wrapper = new MessageValidationWrapper(json, [spec1, spec2], null)
        topic.getContentType() >> ContentType.JSON

        when:
        def result = filteringService.isFiltered(wrapper, topic)

        then:
        !result.filtered
        0 * schemaRepository._
    }

    def "should filter avro message"() {
        given:
        def rawMessage = '''
        {
            "id": "0001",
            "name": "XYZ"
        }
        '''
        def wrapper = new MessageValidationWrapper(rawMessage, [spec1, spec2], null)


        when:
        def result = filteringService.isFiltered(wrapper, topic)

        then:
        result.filtered
        1 * schemaRepository.getAvroSchema(topic) >> new CompiledSchema(schema, SchemaVersion.valueOf(1))
        topic.getContentType() >> ContentType.AVRO
    }

    def "should not filter avro message"() {
        given:
        def rawMessage = '''
        {
            "id": "0001",
            "name": "abc"
        }
        '''
        def wrapper = new MessageValidationWrapper(rawMessage, [spec1, spec2], null)

        when:
        def result = filteringService.isFiltered(wrapper, topic)

        then:
        !result.filtered
        1 * schemaRepository.getAvroSchema(topic) >> new CompiledSchema(schema, SchemaVersion.valueOf(1))
        topic.getContentType() >> ContentType.AVRO

    }

    def "should get specific version of avro schema"() {
        given:
        def rawMessage = '''
        {
            "id": "0001",
            "name": "abc"
        }
        '''
        def wrapper = new MessageValidationWrapper(rawMessage, [spec1, spec2], 42)

        when:
        def result = filteringService.isFiltered(wrapper, topic)

        then:
        !result.filtered
        1 * schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(42)) >> new CompiledSchema(schema, SchemaVersion.valueOf(42))
        topic.getContentType() >> ContentType.AVRO
    }

    def "should throw exception when retrieving wrong version of avro schema"() {
        def rawMessage = '''
        {
            "id": "0001",
            "name": "abc"
        }
        '''
        def wrapper = new MessageValidationWrapper(rawMessage, [spec1, spec2], 1)

        when:
        filteringService.isFiltered(wrapper, topic)

        then:
        1 * schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(1)) >> {
            throw new UnknownSchemaVersionException(topic, SchemaVersion.valueOf(1))
        }
        topic.getContentType() >> ContentType.AVRO
        thrown UnknownSchemaVersionException
    }

    //todo test with wrong schemaVersion?
}
