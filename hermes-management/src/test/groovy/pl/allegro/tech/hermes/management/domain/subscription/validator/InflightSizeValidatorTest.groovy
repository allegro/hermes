package pl.allegro.tech.hermes.management.domain.subscription.validator

import jakarta.validation.ConstraintViolationException
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionPolicy
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions
import pl.allegro.tech.hermes.management.domain.auth.TestRequestUser
import pl.allegro.tech.hermes.management.domain.owner.validator.OwnerIdValidator
import pl.allegro.tech.hermes.management.domain.topic.TopicService
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class InflightSizeValidatorTest extends Specification {

    @Shared
    def ownerIdValidator = Stub(OwnerIdValidator)
    @Shared
    def topicService = Stub(TopicService)
    @Shared
    def subscriptionRepository = Stub(SubscriptionRepository)
    @Shared
    def endpointOwnershipValidator = Stub(EndpointOwnershipValidator)

    @Shared
    private static regularUser = new TestRequestUser("regularUser", false, true)
    @Shared
    private static admin = new TestRequestUser("admin", true)

    @Subject
    @Shared
    SubscriptionValidator subscriptionValidator = new SubscriptionValidator(
            ownerIdValidator,
            new ApiPreconditions(),
            topicService,
            subscriptionRepository,
            [],
            endpointOwnershipValidator,
            []
    )

    def "creating subscription with inflight size should not be allowed for regular users"() {
        given:
        def subscription = subscriptionWithInflight(100)
        when:
        subscriptionValidator.checkCreation(subscription, regularUser)

        then:
        def exception = thrown(SubscriptionValidationException)
        exception.message == "Inflight size can't be set by non admin users"
    }

    def "creating subscription with inflight size should be allowed for admin users"() {
        given:
        def subscription = subscriptionWithInflight(100)

        when:
        subscriptionValidator.checkCreation(subscription, admin)

        then:
        noExceptionThrown()
    }

    def "creating subscription with inflight size less than 1 should not be allowed"() {
        given:
        def subscription = subscriptionWithInflight(inflightSize)
        when:
        subscriptionValidator.checkCreation(subscription, admin)

        then:
        def exception = thrown(ConstraintViolationException)
        exception.message == "serialSubscriptionPolicy.inflightSize: must be greater than or equal to 1"

        where:
        inflightSize << [0, -1]
    }

    def "creating subscription without inflight size should be allowed for regular and admin users"() {
        given:
        def subscription = subscriptionWithInflight(null)
        when:
        subscriptionValidator.checkCreation(subscription, user)

        then:
        noExceptionThrown()

        where:
        user << [regularUser, admin]
    }

    def "changing inflight should not be allowed for users"() {
        given:
        def previous = subscriptionWithInflight(previousInflight)
        def updated = subscriptionWithInflight(updatedInflight)
        when:
        subscriptionValidator.checkModification(updated, regularUser, previous)

        then:
        def exception = thrown(SubscriptionValidationException)
        exception.message == message

        where:
        previousInflight | updatedInflight || message
        null             | 60              || "Inflight size can't be changed by non admin users. Changed from: null, to: 60"
        60               | null            || "Inflight size can't be changed by non admin users. Changed from: 60, to: null"
        60               | 120             || "Inflight size can't be changed by non admin users. Changed from: 60, to: 120"
    }

    def "changing inflight should be allowed for admins"() {
        given:
        def previous = subscriptionWithInflight(previousInflight)
        def updated = subscriptionWithInflight(updatedInflight)
        when:
        subscriptionValidator.checkModification(updated, admin, previous)

        then:
        noExceptionThrown()

        where:
        previousInflight | updatedInflight
        null             | 60
        60               | null
        60               | 120
    }

    def "updating subscription with non default inflight should be allowed for all users"() {
        given:
        def previous = subscriptionWithInflight(120, "lorem ipsum")
        def updated = subscriptionWithInflight(120, "dolor sit amet")

        when:
        subscriptionValidator.checkModification(updated, user, previous)

        then:
        noExceptionThrown()

        where:
        user << [regularUser, admin]
    }

    def "resetting subscription policy should be allowed for all users"() {
        def previous = subscriptionWithInflight(120)
        def updated = subscriptionWithInflight(null)
        updated.serialSubscriptionPolicy = null

        when:
        subscriptionValidator.checkModification(updated, user, previous)

        then:
        noExceptionThrown()

        where:
        user << [regularUser, admin]
    }

    def "changing subscription policy from null to policy with not null inflight should not be allowed for regular user"() {
        def previous = subscriptionWithInflight(null)
        previous.serialSubscriptionPolicy = null

        def updated = subscriptionWithInflight(120)

        when:
        subscriptionValidator.checkModification(updated, regularUser, previous)

        then:
        def exception = thrown(SubscriptionValidationException)
        exception.message == "Inflight size can't be changed by non admin users. Changed from: null, to: 120"
    }

    def "changing subscription policy from null to policy with not null inflight should  be allowed for admin"() {
        def previous = subscriptionWithInflight(null)
        previous.serialSubscriptionPolicy = null

        def updated = subscriptionWithInflight(120)

        when:
        subscriptionValidator.checkModification(updated, admin, previous)

        then:
        noExceptionThrown()
    }

    private static Subscription subscriptionWithInflight(Integer inflightSize, String description = "lorem ipsum") {
        return subscription("group.topic", "subscription")
                .withDescription(description)
                .withSubscriptionPolicy(
                        SubscriptionPolicy.Builder.subscriptionPolicy()
                                .withInflightSize(inflightSize)
                                .build()
                ).build()
    }
}
