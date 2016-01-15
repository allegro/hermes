package pl.allegro.tech.hermes.management.domain.topic.validator

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.domain.topic.schema.CouldNotLoadSchemaException
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository
import pl.allegro.tech.hermes.test.helper.avro.AvroUser
import spock.lang.Specification

import static pl.allegro.tech.hermes.api.Topic.Builder.topic

class TopicValidatorTest extends Specification {

    def avroSchemaRepository = Stub(SchemaRepository)
    def topicValidator = new TopicValidator(avroSchemaRepository)

    def "should not fail when creating valid topic"() {
        when:
        topicValidator.ensureCreatedTopicIsValid(topic().applyDefaults().build())

        then:
        noExceptionThrown()
    }

    def "should fail when creating topic with migratedFromJsonType flag set"() {
        given:
        def migratedTopic = topic().applyDefaults().migratedFromJsonType().build()

        when:
        topicValidator.ensureCreatedTopicIsValid(migratedTopic)

        then:
        thrown TopicValidationException
    }

    def "should not fail when updating valid topic"() {
        given:
        def validTopic = topic().applyDefaults().withTrackingEnabled(false).build()
        def updatedValidTopic = topic().applyPatch(validTopic).withTrackingEnabled(true).build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedValidTopic, validTopic)

        then:
        noExceptionThrown()
    }

    def "should fail when changing content type from #previousType to #updatedType without setting migratedToJsonType flag"() {
        given:
        def jsonTopic = topic().applyDefaults().withContentType(previousType).build()
        def updatedTopic = topic().applyPatch(jsonTopic).withContentType(updatedType).build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedTopic, jsonTopic)

        then:
        thrown TopicValidationException

        where:
        previousType     | updatedType
        ContentType.JSON | ContentType.AVRO
        ContentType.AVRO | ContentType.JSON
    }

    def "should fail when changing content type from avro to json and unsetting migratedToJsonType flag"() {
        given:
        def jsonTopic = topic().applyDefaults().withContentType(ContentType.AVRO).migratedFromJsonType().build()
        def updatedTopic = topic().applyDefaults().withContentType(ContentType.JSON).build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedTopic, jsonTopic)

        then:
        thrown TopicValidationException
    }

    def "should fail when changing content type from json to avro and avro schema not available"() {
        given:
        def jsonTopic = topic().applyDefaults().withContentType(ContentType.JSON).build()
        def migratedTopic = topic().applyDefaults().withContentType(ContentType.AVRO).migratedFromJsonType().build()
        avroSchemaRepository.getSchema(migratedTopic) >> { throw new CouldNotLoadSchemaException("", new RuntimeException()) }

        when:
        topicValidator.ensureUpdatedTopicIsValid(migratedTopic, jsonTopic)

        then:
        thrown TopicValidationException
    }

    def "should not fail when changing content type from json to avro and avro schema is available"() {
        given:
        def jsonTopic = topic().applyDefaults().withContentType(ContentType.JSON).build()
        def migratedTopic = topic().applyDefaults().withContentType(ContentType.AVRO).migratedFromJsonType().build()
        avroSchemaRepository.getSchema(migratedTopic) >> new AvroUser().schema

        when:
        topicValidator.ensureUpdatedTopicIsValid(migratedTopic, jsonTopic)

        then:
        noExceptionThrown()
    }

}
