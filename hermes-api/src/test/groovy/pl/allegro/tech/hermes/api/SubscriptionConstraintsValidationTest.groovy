package pl.allegro.tech.hermes.api

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.hibernate.validator.internal.engine.ConstraintViolationImpl
import spock.lang.Specification

class SubscriptionConstraintsValidationTest extends Specification {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator()

    def "consumers number has to be greater than zero"() {
        given:
        def subscriptionConstraints = new SubscriptionConstraints(
            "group.topic\$subscription",
            new Constraints(consumersNumber, "Some reason")
        )

        when:
        Set<ConstraintViolationImpl<SubscriptionConstraints>> violations = validator.validate(subscriptionConstraints)

        then:
        violations.propertyPath*.toString() == ["constraints.consumersNumber"]
        violations*.interpolatedMessage == ["must be greater than or equal to 1"]

        where:
        consumersNumber << [-100, -1, 0]
    }

    def "reason message length has to be max 1024"() {
        given:
        def subscriptionConstraints = new SubscriptionConstraints(
            "group.topic\$subscription",
            new Constraints(1, reason)
        )

        when:
        Set<ConstraintViolationImpl<SubscriptionConstraints>> violations = validator.validate(subscriptionConstraints)

        then:
        violations.propertyPath*.toString() == ["constraints.reason"]
        violations*.interpolatedMessage == ["size must be between 0 and 1024"]

        where:
        reason << [
            "r".repeat(1025),
            "r".repeat(2048),
            "r".repeat(10000)
        ]
    }

    def "there shouldn't be any violations for valid inputs"() {
        given:
        def subscriptionConstraints = new SubscriptionConstraints(
            "group.topic\$subscription",
            new Constraints(consumersNumber, reason)
        )

        when:
        Set<ConstraintViolationImpl<SubscriptionConstraints>> violations = validator.validate(subscriptionConstraints)

        then:
        violations.isEmpty()

        where:
        consumersNumber | reason
        1               | "r".repeat(1023)
        10              | ""
        100             | null
        100             | "r"
    }
}
