package pl.allegro.tech.hermes.management.domain.topic.validator

import jakarta.validation.ConstraintViolationException
import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.api.TopicLabel
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions
import pl.allegro.tech.hermes.management.config.TopicProperties
import pl.allegro.tech.hermes.management.domain.auth.TestRequestUser
import pl.allegro.tech.hermes.management.domain.owner.validator.OwnerIdValidationException
import pl.allegro.tech.hermes.management.domain.owner.validator.OwnerIdValidator
import pl.allegro.tech.hermes.schema.CompiledSchema
import pl.allegro.tech.hermes.schema.CouldNotLoadSchemaException
import pl.allegro.tech.hermes.schema.SchemaRepository
import pl.allegro.tech.hermes.test.helper.avro.AvroUser
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class TopicValidatorTest extends Specification {

    static MANAGABLE = { true }
    static NOT_MANAGABLE = { false }
    private static USER = new TestRequestUser("username", false)

    static Set<TopicLabel> allowedLabels

    def schemaRepository = Stub(SchemaRepository)
    def ownerDescriptorValidator = Stub(OwnerIdValidator)
    def contentTypeWhitelistValidator = Stub(ContentTypeValidator)
    def apiPreconditions = Stub(ApiPreconditions)
    def topicLabelsValidator
    def topicProperties = new TopicProperties()

    @Subject
    TopicValidator topicValidator

    def setup() {
        allowedLabels = []
        TopicProperties topicProperties = new TopicProperties()
        topicProperties.setAllowedTopicLabels(allowedLabels)
        topicLabelsValidator = new TopicLabelsValidator(topicProperties)

        topicValidator = new TopicValidator(ownerDescriptorValidator, contentTypeWhitelistValidator, topicLabelsValidator, schemaRepository, apiPreconditions, topicProperties)
    }

    def "topic with basic properties when creating should be valid"() {
        when:
        topicValidator.ensureCreatedTopicIsValid(topic('group.topic').build(), USER, MANAGABLE)

        then:
        noExceptionThrown()
    }

    def "topic not meeting preconditions when creating should be invalid"() {
        given:
        apiPreconditions.checkConstraints(_, _) >> { throw new ConstraintViolationException("failed", Collections.emptySet()) }

        when:
        topicValidator.ensureCreatedTopicIsValid(topic('group.invalid').build(), USER, MANAGABLE)

        then:
        thrown ConstraintViolationException
    }

    def "topic with migratedFromJsonType flag set when creating should be invalid"() {
        given:
        def migratedTopic = topic('group.topic').migratedFromJsonType().build()

        when:
        topicValidator.ensureCreatedTopicIsValid(migratedTopic, USER, MANAGABLE)

        then:
        thrown TopicValidationException
    }

    def "topic with invalid owner when creating should be invalid"() {
        given:
        ownerDescriptorValidator.check(_) >> { throw new OwnerIdValidationException("failed") }

        when:
        topicValidator.ensureCreatedTopicIsValid(topic('group.topic').build(), USER, MANAGABLE)

        then:
        thrown OwnerIdValidationException
    }

    def "topic with owner that doesn't include the creator should be invalid"() {
        when:
        topicValidator.ensureCreatedTopicIsValid(topic('group.topic').build(), USER, NOT_MANAGABLE)

        then:
        thrown TopicValidationException
    }

    def "topic with not allowed content type should be invalid"() {
        given:
        contentTypeWhitelistValidator.check(_) >> { throw new TopicValidationException("failed") }

        when:
        topicValidator.ensureCreatedTopicIsValid(topic('group.topic').build(), USER, MANAGABLE)

        then:
        thrown TopicValidationException
    }

    def "topic when doing a basic update should be valid"() {
        given:
        Topic validTopic = topic('group.topic').withTrackingEnabled(false).build()
        Topic updatedValidTopic = topic('group.topic').withTrackingEnabled(true).build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedValidTopic, validTopic, USER)

        then:
        noExceptionThrown()
    }

    def "topic changing content type from #previousType to #updatedType without setting migratedToJsonType flag should be invalid"() {
        given:
        Topic originalTopic = topic('group.topic').withContentType(previousType).build()
        Topic updatedTopic = topic('group.topic').withContentType(updatedType).build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedTopic, originalTopic, USER)

        then:
        thrown TopicValidationException

        where:
        previousType     | updatedType
        ContentType.JSON | ContentType.AVRO
        ContentType.AVRO | ContentType.JSON
    }

    def "topic changing content type from avro to json and unsetting migratedToJsonType flag should be invalid"() {
        given:
        def jsonTopic = topic('group.topic').withContentType(ContentType.AVRO).migratedFromJsonType().build()
        def updatedTopic = topic('group.topic').withContentType(ContentType.JSON).build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedTopic, jsonTopic, USER)

        then:
        thrown TopicValidationException
    }

    def "topic changing content type from json to avro when avro schema not available should be invalid"() {
        given:
        def jsonTopic = topic('group.topic').withContentType(ContentType.JSON).build()
        def migratedTopic = topic('group.topic').withContentType(ContentType.AVRO).migratedFromJsonType().build()
        schemaRepository.getLatestAvroSchema(migratedTopic) >> { throw new CouldNotLoadSchemaException(new RuntimeException()) }

        when:
        topicValidator.ensureUpdatedTopicIsValid(migratedTopic, jsonTopic, USER)

        then:
        thrown TopicValidationException
    }

    def "topic changing content type from json to avro when avro schema is available should be valid"() {
        given:
        def jsonTopic = topic('group.topic').withContentType(ContentType.JSON).build()
        def migratedTopic = topic('group.topic').withContentType(ContentType.AVRO).migratedFromJsonType().build()
        schemaRepository.getLatestAvroSchema(migratedTopic) >> CompiledSchema.of(new AvroUser().schema, 1, 1)

        when:
        topicValidator.ensureUpdatedTopicIsValid(migratedTopic, jsonTopic, USER)

        then:
        noExceptionThrown()
    }

    def "topic with invalid owner when updating should be invalid"() {
        given:
        ownerDescriptorValidator.check(_) >> { throw new OwnerIdValidationException("failed") }

        when:
        topicValidator.ensureUpdatedTopicIsValid(
            topic('group.topic').withDescription("updated").build(),
            topic('group.topic').build(),
            USER
        )

        then:
        thrown OwnerIdValidationException
    }

    @Unroll
    def "topic with allowed labels should be valid during creation"() {
        given:
        allowedLabels.addAll(givenAllowedLabels)

        when:
        topicValidator.ensureCreatedTopicIsValid(
            topic('group.topic').withLabels(createdTopicLabels as Set).build(),
            USER,
            MANAGABLE
        )

        then:
        noExceptionThrown()

        where:
        givenAllowedLabels                         | createdTopicLabels
        []                                         | []
        [l('label-1')]                             | []
        [l('label-1')]                             | [l('label-1')]
        [l('label-1'), l('label-2')]               | []
        [l('label-1'), l('label-2')]               | [l('label-1')]
        [l('label-1'), l('label-2')]               | [l('label-1'), l('label-2')]
        [l('label-1'), l('label-2'), l('label-3')] | []
        [l('label-1'), l('label-2'), l('label-3')] | [l('label-1')]
        [l('label-1'), l('label-2'), l('label-3')] | [l('label-1'), l('label-2')]
        [l('label-1'), l('label-2'), l('label-3')] | [l('label-1'), l('label-2'), l('label-3')]
    }

    @Unroll
    def "topic with at least one disallowed label should not be valid during creation"() {
        given:
        allowedLabels.addAll(givenAllowedLabels)

        when:
        topicValidator.ensureCreatedTopicIsValid(
            topic('group.topic').withLabels(createdTopicLabels as Set).build(),
            USER,
            MANAGABLE
        )

        then:
        thrown TopicValidationException

        where:
        givenAllowedLabels                         | createdTopicLabels
        []                                         | [l('some-random-label')]
        [l('label-1')]                             | [l('some-random-label')]
        [l('label-1')]                             | [l('label-1'), l('some-random-label')]
        [l('label-1'), l('label-2')]               | [l('some-random-label')]
        [l('label-1'), l('label-2')]               | [l('label-1'), l('some-random-label')]
        [l('label-1'), l('label-2')]               | [l('label-1'), l('label-2'), l('some-random-label')]
        [l('label-1'), l('label-2'), l('label-3')] | [l('some-random-label')]
        [l('label-1'), l('label-2'), l('label-3')] | [l('label-1'), l('some-random-label')]
        [l('label-1'), l('label-2'), l('label-3')] | [l('label-1'), l('label-2'), l('some-random-label')]
        [l('label-1'), l('label-2'), l('label-3')] | [l('label-1'), l('label-2'), l('label-3'), l('some-random-label')]
    }

    @Unroll
    def "topic with allowed labels should be valid during update"() {
        given:
        allowedLabels.addAll(givenAllowedLabels)

        when:
        topicValidator.ensureUpdatedTopicIsValid(
            topic('group.topic').withLabels(updatedTopicLabels as Set).build(),
            topic('group.topic').build(),
            USER
        )

        then:
        noExceptionThrown()

        where:
        givenAllowedLabels                         | updatedTopicLabels
        []                                         | []
        [l('label-1')]                             | []
        [l('label-1')]                             | [l('label-1')]
        [l('label-1'), l('label-2')]               | []
        [l('label-1'), l('label-2')]               | [l('label-1')]
        [l('label-1'), l('label-2')]               | [l('label-1'), l('label-2')]
        [l('label-1'), l('label-2'), l('label-3')] | []
        [l('label-1'), l('label-2'), l('label-3')] | [l('label-1')]
        [l('label-1'), l('label-2'), l('label-3')] | [l('label-1'), l('label-2')]
        [l('label-1'), l('label-2'), l('label-3')] | [l('label-1'), l('label-2'), l('label-3')]
    }

    @Unroll
    def "topic with at least one disallowed label should not be valid during update"() {
        given:
        allowedLabels.addAll(givenAllowedLabels)

        when:
        topicValidator.ensureUpdatedTopicIsValid(
            topic('group.topic').withLabels(updatedTopicLabels as Set).build(),
            topic('group.topic').build(),
            USER
        )

        then:
        thrown TopicValidationException

        where:
        givenAllowedLabels                         | updatedTopicLabels
        []                                         | [l('some-random-label')]
        [l('label-1')]                             | [l('some-random-label')]
        [l('label-1')]                             | [l('label-1'), l('some-random-label')]
        [l('label-1'), l('label-2')]               | [l('some-random-label')]
        [l('label-1'), l('label-2')]               | [l('label-1'), l('some-random-label')]
        [l('label-1'), l('label-2')]               | [l('label-1'), l('label-2'), l('some-random-label')]
        [l('label-1'), l('label-2'), l('label-3')] | [l('some-random-label')]
        [l('label-1'), l('label-2'), l('label-3')] | [l('label-1'), l('some-random-label')]
        [l('label-1'), l('label-2'), l('label-3')] | [l('label-1'), l('label-2'), l('some-random-label')]
        [l('label-1'), l('label-2'), l('label-3')] | [l('label-1'), l('label-2'), l('label-3'), l('some-random-label')]
    }

    private static l(String value) {
        return new TopicLabel(value)
    }
}
