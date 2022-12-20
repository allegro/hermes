package pl.allegro.tech.hermes.management.domain.subscription.validator

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

import javax.validation.ConstraintViolationException

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
        def exception = thrown(ConstraintViolationException)
        exception.message == "serialSubscriptionPolicy.inflightSize: must be null"
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

    private static Subscription subscriptionWithInflight(Integer inflightSize) {
        return subscription("group.topic", "subscription")
                .withSubscriptionPolicy(
                        SubscriptionPolicy.Builder.subscriptionPolicy()
                                .withInflightSize(inflightSize)
                                .build()
                ).build()
    }
}
