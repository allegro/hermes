package pl.allegro.tech.hermes.api.constraints

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.MessageFilterSpecification
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import spock.lang.Specification

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator

class MessageFilterValidatorTest extends Specification {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    def "should not return violations for empty filter list"() {

        given:
            Subscription subscription = createSubscription()

        when:
            Set<ConstraintViolation<Subscription>> violations = validator.validate(subscription)

        then:
            violations.isEmpty()
    }

    def "should not return violations for valid content type filter type combinations"() {

        given:
            Subscription subscription = createSubscription(contentType, filterTypes)

        when:
            Set<ConstraintViolation<Subscription>> violations = validator.validate(subscription)

        then:
            violations.isEmpty() == expectedResult

        where:
            contentType      | filterTypes              || expectedResult
            ContentType.JSON | ["jsonpath"]             || true
            ContentType.JSON | ["jsonpath", "jsonpath"] || true
            ContentType.JSON | ["header"]               || true
            ContentType.JSON | ["jsonpath", "header"]   || true
            ContentType.AVRO | ["avropath"]             || true
            ContentType.AVRO | ["header"]               || true
            ContentType.AVRO | ["avropath", "avropath"] || true
            ContentType.AVRO | ["avropath", "header"]   || true
    }

    def "should return violations for invalid content type filter type combinations"() {

        given:
            Subscription subscription = createSubscription(contentType, filterTypes)

        when:
            Set<ConstraintViolation<Subscription>> violations = validator.validate(subscription)

        then:
            violations.isEmpty() == expectedResult

        where:
            contentType      | filterTypes              || expectedResult
            ContentType.AVRO | ["jsonpath"]             || false
            ContentType.AVRO | ["jsonpath", "avropath"] || false
            ContentType.JSON | ["avropath"]             || false
            ContentType.JSON | ["avropath", "jsonpath"] || false
    }

    private Subscription createSubscription(ContentType contentType = ContentType.JSON, List<String> filterTypes = []) {
        SubscriptionBuilder builder = SubscriptionBuilder.subscription("com.example", "testName")
                .withContentType(contentType)
        filterTypes.each { builder.withFilter(filterOfType(it)) }
        return builder.build()
    }

    private MessageFilterSpecification filterOfType(String type) {
        return new MessageFilterSpecification([type: type])
    }
}
