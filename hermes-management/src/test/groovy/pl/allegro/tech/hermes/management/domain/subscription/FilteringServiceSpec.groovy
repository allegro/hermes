package pl.allegro.tech.hermes.management.domain.subscription

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.MessageFilterSpecification
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.filtering.MessageFilters
import pl.allegro.tech.hermes.common.filtering.avro.AvroPathSubscriptionMessageFilterCompiler
import pl.allegro.tech.hermes.common.filtering.json.JsonPathSubscriptionMessageFilterCompiler
import pl.allegro.tech.hermes.management.domain.message.filtering.FilteringConversionException
import pl.allegro.tech.hermes.management.domain.message.filtering.InvalidFilterTypeException
import pl.allegro.tech.hermes.api.MessageValidationWrapper
import pl.allegro.tech.hermes.schema.CompiledSchema
import pl.allegro.tech.hermes.schema.SchemaRepository
import pl.allegro.tech.hermes.schema.SchemaVersion
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader
import spock.lang.Specification

class FilteringServiceSpec extends Specification {

    MessageFilters messageFilters = new MessageFilters([], [
            new AvroPathSubscriptionMessageFilterCompiler(),
            new JsonPathSubscriptionMessageFilterCompiler()
    ])
    SchemaRepository schemaRepository = Mock()
    Topic topic = Mock()

    def filteringService = new FilteringService(messageFilters, schemaRepository)
    def jsonSpec1 = new MessageFilterSpecification([type: "jsonpath", path: ".id", matcher: "0001"])
    def jsonSpec2 = new MessageFilterSpecification([type: "jsonpath", path: ".name", matcher: "abc"])

    def avroSpec1 = new MessageFilterSpecification([type: "avropath", path: ".id", matcher: "0001"])
    def avroSpec2 = new MessageFilterSpecification([type: "avropath", path: ".name", matcher: "abc"])

    def schema = AvroUserSchemaLoader.load("/simple.avsc")

    def "should filter json message when any filter fails matching"() {
        given:
        def json = '''
        {
            "id": "0001",
            "name": "XYZ"
        }
        '''
        def wrapper = new MessageValidationWrapper(json, [jsonSpec1, jsonSpec2], null)
        topic.getContentType() >> ContentType.JSON

        expect:
        filteringService.isFiltered(wrapper, topic).filtered
    }

    def "should not filter json message when all filters match"() {
        given:
        def json = '''
        {
            "id": "0001",
            "name": "abc"
        }
        '''
        def wrapper = new MessageValidationWrapper(json, [jsonSpec1, jsonSpec2], null)
        topic.getContentType() >> ContentType.JSON

        expect:
        !filteringService.isFiltered(wrapper, topic).filtered
    }

    def "should filter avro message when any filter fails matching"() {
        given:
        def rawMessage = '''
        {
            "id": "0001",
            "name": "XYZ"
        }
        '''
        def wrapper = new MessageValidationWrapper(rawMessage, [avroSpec1, avroSpec2], null)


        when:
        def result = filteringService.isFiltered(wrapper, topic)

        then:
        result.filtered
        schemaRepository.getLatestAvroSchema(topic) >> new CompiledSchema(schema, SchemaVersion.valueOf(1))
        topic.getContentType() >> ContentType.AVRO
    }

    def "should not filter avro message when all filters match"() {
        given:
        def rawMessage = '''
        {
            "id": "0001",
            "name": "abc"
        }
        '''
        def wrapper = new MessageValidationWrapper(rawMessage, [avroSpec1, avroSpec2], null)

        when:
        def result = filteringService.isFiltered(wrapper, topic)

        then:
        !result.filtered
        schemaRepository.getLatestAvroSchema(topic) >> new CompiledSchema(schema, SchemaVersion.valueOf(1))
        topic.getContentType() >> ContentType.AVRO

    }

    def "should throw InvalidFilterTypeException when invalid content type for filter is provided"() {
        def rawMessage = '''
        {
            "id": "0001",
            "name": "abc"
        }
        '''
        def spec = new MessageFilterSpecification([type: "jsonpath", path: ".name", matcher: "abc"])
        def wrapper = new MessageValidationWrapper(rawMessage, [spec], null)

        when:
        filteringService.isFiltered(wrapper, topic)

        then:
        schemaRepository.getLatestAvroSchema(topic) >> new CompiledSchema(schema, SchemaVersion.valueOf(1))
        topic.getContentType() >> ContentType.AVRO
        thrown InvalidFilterTypeException
    }

    def "should throw exception when converting using wrong schema"() {
        given:
        def rawMessage = '''
        {
            "id": "0001"
        }
        '''
        def wrapper = new MessageValidationWrapper(rawMessage, [avroSpec1, avroSpec2], 1)

        when:
        filteringService.isFiltered(wrapper, topic)

        then:
        schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(1)) >> new CompiledSchema(schema, SchemaVersion.valueOf(1))
        topic.getContentType() >> ContentType.AVRO
        thrown FilteringConversionException
    }
}
