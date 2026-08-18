package pl.allegro.tech.hermes.management.domain.topic

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.RawSchemaWithMetadata
import pl.allegro.tech.hermes.domain.topic.TopicRepository
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaService
import pl.allegro.tech.hermes.schema.SubjectNamingStrategy
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification

import java.time.Clock

class TopicServiceSpec extends Specification {

    TopicRepository topicRepository = Stub()
    SchemaService schemaService = Mock()
    SubjectNamingStrategy subjectNamingStrategy = Mock()
    TopicService topicService = new TopicService(
            null, topicRepository, null, null, schemaService, subjectNamingStrategy, null, null, null, Clock.systemUTC(),
            null, null, null, null, null)

    def "should enrich an avro topic with active and available schema versions"() {
        given:
        def topic = TopicBuilder.topic("group.topic").withContentType(ContentType.AVRO).build()
        topicRepository.getTopicDetails(topic.name) >> topic
        schemaService.getLatestSchema(topic.qualifiedName) >> Optional.of(RawSchemaWithMetadata.of("schema", 101, 3))
        schemaService.getVersionsOrEmptyOnError(topic.qualifiedName) >> [1, 2, 3]
        subjectNamingStrategy.apply(topic.name) >> "namespace_group.topic-value"

        when:
        def result = topicService.getTopicWithSchema(topic.name)

        then:
        result.topic.schema == "schema"
        result.schemaVersion == 3
        result.availableSchemaVersions == [1, 2, 3]
        result.schemaSubject == "namespace_group.topic-value"
    }

    def "should return null schema metadata for a json topic without calling schema service"() {
        given:
        def topic = TopicBuilder.topic("group.topic").withContentType(ContentType.JSON).build()
        topicRepository.getTopicDetails(topic.name) >> topic

        when:
        def result = topicService.getTopicWithSchema(topic.name)

        then:
        result.topic.schema == null
        result.schemaVersion == null
        result.availableSchemaVersions.empty
        result.schemaSubject == null
        0 * schemaService._
    }

    def "should retain active schema metadata when schema version history is unavailable"() {
        given:
        def topic = TopicBuilder.topic("group.topic").withContentType(ContentType.AVRO).build()
        topicRepository.getTopicDetails(topic.name) >> topic
        schemaService.getLatestSchema(topic.qualifiedName) >> Optional.of(RawSchemaWithMetadata.of("schema", 101, 3))
        schemaService.getVersionsOrEmptyOnError(topic.qualifiedName) >> []
        subjectNamingStrategy.apply(topic.name) >> "namespace_group.topic-value"

        when:
        def result = topicService.getTopicWithSchema(topic.name)

        then:
        result.topic.schema == "schema"
        result.schemaVersion == 3
        result.availableSchemaVersions.empty
        result.schemaSubject == "namespace_group.topic-value"
    }
}
