package pl.allegro.tech.hermes.management.domain.topic.validator

import pl.allegro.tech.hermes.api.RetentionTime
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions
import pl.allegro.tech.hermes.management.config.TopicProperties
import pl.allegro.tech.hermes.management.domain.auth.TestRequestUser
import pl.allegro.tech.hermes.management.domain.owner.validator.OwnerIdValidator
import pl.allegro.tech.hermes.schema.SchemaRepository
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

import static java.util.concurrent.TimeUnit.DAYS
import static java.util.concurrent.TimeUnit.HOURS
import static java.util.concurrent.TimeUnit.MINUTES
import static java.util.concurrent.TimeUnit.SECONDS
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class TopicValidatorWithRealApiPreconditionsTest extends Specification {

    private static MANAGEABLE = { true }
    private static regularUser = new TestRequestUser("regularUser", false)
    private static admin = new TestRequestUser("admin", true)

    def schemaRepository = Stub(SchemaRepository)
    def ownerDescriptorValidator = Stub(OwnerIdValidator)
    def contentTypeWhitelistValidator = Stub(ContentTypeValidator)
    def topicLabelsValidator = Stub(TopicLabelsValidator)
    def topicProperties = new TopicProperties()

    @Subject
    def topicValidator = new TopicValidator(ownerDescriptorValidator, contentTypeWhitelistValidator, topicLabelsValidator, schemaRepository, new ApiPreconditions(), topicProperties)

    @Unroll
    def "creating and updating topic with up to 7 days retention time should be valid"() {
        given:
        def topic = topic('group.topic')
                .withRetentionTime(retentionTime)
                .build()

        when:
        topicValidator.ensureCreatedTopicIsValid(topic, user, MANAGEABLE)

        then:
        noExceptionThrown()

        where:
        retentionTime                    | user
        new RetentionTime(1, MINUTES)    | admin
        new RetentionTime(1, MINUTES)    | regularUser
        new RetentionTime(1337, MINUTES) | admin
        new RetentionTime(1337, MINUTES) | regularUser
        new RetentionTime(24, HOURS)     | admin
        new RetentionTime(24, HOURS)     | regularUser
        new RetentionTime(72, HOURS)     | admin
        new RetentionTime(72, HOURS)     | regularUser
        new RetentionTime(1, DAYS)       | admin
        new RetentionTime(1, DAYS)       | regularUser
        new RetentionTime(7, DAYS)       | admin
        new RetentionTime(7, DAYS)       | regularUser
    }

    def "creating topic with over 7 days of retention time should be invalid for regular user"() {
        given:
        def topic = topic('group.topic')
                .withRetentionTime(retentionTime)
                .build()

        when:
        topicValidator.ensureCreatedTopicIsValid(topic, regularUser, MANAGEABLE)

        then:
        def exception = thrown(TopicValidationException)
        exception.message == "Retention time larger than 7 days can't be configured by non admin users"

        where:
        retentionTime << [
                new RetentionTime(8, DAYS),
                new RetentionTime(7 * 24 + 1, HOURS),
                new RetentionTime(7 * 24 * 60 + 1, MINUTES),
                new RetentionTime(7 * 24 * 60 * 60 + 1, SECONDS)
        ]
    }

    def "creating topic with over 7 days retention time should be valid for admin"() {
        given:
        def topic = topic('group.topic')
                .withRetentionTime(retentionTime)
                .build()

        when:
        topicValidator.ensureCreatedTopicIsValid(topic, admin, MANAGEABLE)

        then:
        noExceptionThrown()

        where:
        retentionTime << [
                new RetentionTime(8, DAYS),
                new RetentionTime(7 * 24 + 1, HOURS),
                new RetentionTime(7 * 24 * 60 + 1, MINUTES),
                new RetentionTime(7 * 24 * 60 * 60 + 1, SECONDS)
        ]
    }

    @Unroll
    def "updating topic with up to 7 days of retention time should be valid"() {
        given:
        def existingTopic = topic('group.topic').build()
        def updatedTopic = topic('group.topic')
                .withRetentionTime(retentionTime)
                .build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedTopic, existingTopic, user)

        then:
        noExceptionThrown()

        where:
        retentionTime                    | user
        new RetentionTime(1, MINUTES)    | admin
        new RetentionTime(1, MINUTES)    | regularUser
        new RetentionTime(1337, MINUTES) | admin
        new RetentionTime(1337, MINUTES) | regularUser
        new RetentionTime(24, HOURS)     | admin
        new RetentionTime(24, HOURS)     | regularUser
        new RetentionTime(72, HOURS)     | admin
        new RetentionTime(72, HOURS)     | regularUser
        new RetentionTime(1, DAYS)       | admin
        new RetentionTime(1, DAYS)       | regularUser
        new RetentionTime(7, DAYS)       | admin
        new RetentionTime(7, DAYS)       | regularUser
    }

    def "updating topic with over 7 days of retention time should be invalid for regular user"() {
        given:
        def existingTopic = topic('group.topic').build()
        def updatedTopic = topic('group.topic')
                .withRetentionTime(retentionTime)
                .build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedTopic, existingTopic, regularUser)

        then:
        def exception = thrown(TopicValidationException)
        exception.message == "Retention time larger than 7 days can't be configured by non admin users"

        where:
        retentionTime << [
                new RetentionTime(8, DAYS),
                new RetentionTime(7 * 24 + 1, HOURS),
                new RetentionTime(7 * 24 * 60 + 1, MINUTES),
                new RetentionTime(7 * 24 * 60 * 60 + 1, SECONDS)
        ]
    }

    def "updating topic with 8 days retention time should be valid for admin"() {
        given:
        def existingTopic = topic('group.topic').build()
        def updatedTopic = topic('group.topic')
                .withRetentionTime(retentionTime)
                .build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedTopic, existingTopic, admin)

        then:
        noExceptionThrown()

        where:
        retentionTime << [
                new RetentionTime(8, DAYS),
                new RetentionTime(7 * 24 + 1, HOURS),
                new RetentionTime(7 * 24 * 60 + 1, MINUTES),
                new RetentionTime(7 * 24 * 60 * 60 + 1, SECONDS)
        ]
    }

    def "updating topic without modifying retention time already exceeding maximum should be valid"() {
        given:
        def existingTopic = topic('group.topic')
                .withRetentionTime(new RetentionTime(8, DAYS))
                .build()
        def updatedTopic = topic('group.topic')
                .withRetentionTime(new RetentionTime(8, DAYS))
                .withDescription("lorem ipsum")
                .build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedTopic, existingTopic, user)

        then:
        noExceptionThrown()

        where:
        user << [regularUser, admin]
    }

    def "updating topic with modifying retention time already exceeding maximum should be invalid for regular user"() {
        given:
        def existingTopic = topic('group.topic')
                .withRetentionTime(new RetentionTime(8, DAYS))
                .build()
        def updatedTopic = topic('group.topic')
                .withRetentionTime(new RetentionTime(12, DAYS))
                .withDescription("lorem ipsum")
                .build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedTopic, existingTopic, regularUser)

        then:
        def exception = thrown(TopicValidationException)
        exception.message == "Retention time larger than 7 days can't be configured by non admin users"
    }

    def "updating topic with modifying retention time already exceeding maximum should be valid for admin"() {
        given:
        def existingTopic = topic('group.topic')
                .withRetentionTime(new RetentionTime(8, DAYS))
                .build()
        def updatedTopic = topic('group.topic')
                .withRetentionTime(new RetentionTime(12, DAYS))
                .withDescription("lorem ipsum")
                .build()

        when:
        topicValidator.ensureUpdatedTopicIsValid(updatedTopic, existingTopic, admin)

        then:
        noExceptionThrown()
    }

    @Unroll
    def "creating a topic with timeUnit smaller than seconds should be invalid"() {
        given:
        def topic = topic('group.topic')
                .withRetentionTime(retentionTime)
                .build()

        when:
        topicValidator.ensureCreatedTopicIsValid(topic, regularUser, MANAGEABLE)

        then:
        def exception = thrown(TopicValidationException)
        exception.message == "Retention time unit must be one of: [SECONDS, MINUTES, HOURS, DAYS]"

        where:
        retentionTime << [
                new RetentionTime(1, TimeUnit.MICROSECONDS),
                new RetentionTime(1, TimeUnit.MILLISECONDS),
                new RetentionTime(1, TimeUnit.NANOSECONDS)
        ]
    }
}
