package pl.allegro.tech.hermes.management.domain.subscription.validator

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.MessageFilterSpecification
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification

class MessageFilterTypeValidatorTest extends Specification {

    private MessageFilterTypeValidator validator = new MessageFilterTypeValidator()

    def "should not return violations for empty filter list"() {

        given:
            Subscription subscription = createSubscription()
            Topic topic = createTopic()

        when:
            validator.check(subscription, topic)

        then:
            noExceptionThrown()
    }

    def "should not return violations for valid content type filter type combinations"() {

        given:
            Subscription subscription = createSubscription(filterTypes)
            Topic topic = createTopic(contentType)

        when:
            validator.check(subscription, topic)

        then:
            noExceptionThrown()

        where:
            contentType      | filterTypes
            ContentType.JSON | ["jsonpath"]
            ContentType.JSON | ["jsonpath", "jsonpath"]
            ContentType.JSON | ["header"]
            ContentType.JSON | ["jsonpath", "header"]
            ContentType.AVRO | ["avropath"]
            ContentType.AVRO | ["header"]
            ContentType.AVRO | ["avropath", "avropath"]
            ContentType.AVRO | ["avropath", "header"]
    }

    def "should return violations for invalid content type filter type combinations"() {

        given:
            Subscription subscription = createSubscription(filterTypes)
            Topic topic = createTopic(contentType)

        when:
            validator.check(subscription, topic)

        then:
            thrown SubscriptionValidationException

        where:
            contentType      | filterTypes
            ContentType.AVRO | ["jsonpath"]
            ContentType.AVRO | ["jsonpath", "avropath"]
            ContentType.JSON | ["avropath"]
            ContentType.JSON | ["avropath", "jsonpath"]
    }

    private Subscription createSubscription(List<String> filterTypes = []) {
        SubscriptionBuilder builder = SubscriptionBuilder.subscription("com.example", "testName")
        filterTypes.each { builder.withFilter(filterOfType(it)) }
        return builder.build()
    }

    private MessageFilterSpecification filterOfType(String type) {
        return new MessageFilterSpecification([type: type])
    }

    private Topic createTopic(ContentType contentType = ContentType.JSON) {
        return TopicBuilder.topic("com.example.testName")
                .withContentType(contentType)
                .build()
    }
}
