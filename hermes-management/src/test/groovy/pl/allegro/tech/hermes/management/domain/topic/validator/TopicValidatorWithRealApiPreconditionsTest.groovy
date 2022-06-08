package pl.allegro.tech.hermes.management.domain.topic.validator

import pl.allegro.tech.hermes.api.RetentionTime
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions
import pl.allegro.tech.hermes.management.domain.auth.TestRequestUser
import pl.allegro.tech.hermes.management.domain.owner.validator.OwnerIdValidator
import pl.allegro.tech.hermes.schema.SchemaRepository
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import javax.validation.ConstraintViolationException

import static java.util.concurrent.TimeUnit.DAYS
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class TopicValidatorWithRealApiPreconditionsTest extends Specification {

    private static MANAGEABLE = { true }
    private static retentionTime7Days = new RetentionTime(7, DAYS)
    private static retentionTime8Days = new RetentionTime(8, DAYS)
    private static regularUser = new TestRequestUser("regularUser", false)
    private static admin = new TestRequestUser("admin", true)

    def schemaRepository = Stub(SchemaRepository)
    def ownerDescriptorValidator = Stub(OwnerIdValidator)
    def contentTypeWhitelistValidator = Stub(ContentTypeValidator)
    def topicLabelsValidator = Stub(TopicLabelsValidator)

    @Subject
    def topicValidator = new TopicValidator(ownerDescriptorValidator, contentTypeWhitelistValidator, topicLabelsValidator, schemaRepository, new ApiPreconditions())

    @Unroll
    def "creating topic with 7 days retention time should be valid"() {
        given:
        def topic = topic('group.topic')
                .withRetentionTime(retentionTime7Days)
                .build()

        when:
        topicValidator.ensureCreatedTopicIsValid(topic, user, MANAGEABLE)

        then:
        noExceptionThrown()

        where:
        user << [regularUser, admin]
    }

    def "creating topic with 8 days retention time should be invalid for regular user"() {
        given:
        def topic = topic('group.topic')
                .withRetentionTime(retentionTime8Days)
                .build()

        when:
        topicValidator.ensureCreatedTopicIsValid(topic, regularUser, MANAGEABLE)

        then:
        thrown ConstraintViolationException
    }

    def "creating topic with 8 days retention time should be valid for admin"() {
        given:
        def topic = topic('group.topic')
                .withRetentionTime(retentionTime8Days)
                .build()

        when:
        topicValidator.ensureCreatedTopicIsValid(topic, admin, MANAGEABLE)

        then:
        noExceptionThrown()
    }

    @Unroll
    def "updating topic with 7 days retention time should be valid"() {
        given:
        def existingTopic = topic('group.topic').build()
        def updatedTopic = topic('group.topic')
                .withRetentionTime(retentionTime7Days)
                .build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedTopic, existingTopic, user)

        then:
        noExceptionThrown()

        where:
        user << [regularUser, admin]
    }

    def "updating topic with 8 days retention time should be invalid for regular user"() {
        given:
        def existingTopic = topic('group.topic').build()
        def updatedTopic = topic('group.topic')
                .withRetentionTime(retentionTime8Days)
                .build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedTopic, existingTopic, regularUser)

        then:
        thrown ConstraintViolationException
    }

    def "updating topic with 8 days retention time should be valid for admin"() {
        given:
        def existingTopic = topic('group.topic').build()
        def updatedTopic = topic('group.topic')
                .withRetentionTime(retentionTime8Days)
                .build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedTopic, existingTopic, admin)

        then:
        noExceptionThrown()
    }
}
