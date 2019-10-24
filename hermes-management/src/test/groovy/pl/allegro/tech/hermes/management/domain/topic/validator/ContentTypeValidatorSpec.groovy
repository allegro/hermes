package pl.allegro.tech.hermes.management.domain.topic.validator

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.management.config.TopicProperties
import spock.lang.Specification
import spock.lang.Unroll

class ContentTypeValidatorSpec extends Specification {

    @Unroll
    def "content type #contentType in whitelist #whitelist should be valid"() {
        given:
        def topicProperties = new TopicProperties()
        topicProperties.setAllowedContentTypes(whitelist)
        def validator = new ContentTypeValidator(topicProperties)

        when:
        validator.check(contentType)

        then:
        noExceptionThrown()

        where:
        whitelist                            | contentType
        [ContentType.AVRO]                   | ContentType.AVRO
        [ContentType.AVRO, ContentType.JSON] | ContentType.AVRO
        [ContentType.JSON]                   | ContentType.JSON
        [ContentType.AVRO, ContentType.JSON] | ContentType.JSON
    }

    @Unroll
    def "content type #contentType not within whitelist #whitelist should be not valid"() {
        given:
        def topicProperties = new TopicProperties()
        topicProperties.setAllowedContentTypes(whitelist)
        def validator = new ContentTypeValidator(topicProperties)

        when:
        validator.check(contentType)

        then:
        thrown TopicValidationException

        where:
        whitelist          | contentType
        [ContentType.AVRO] | ContentType.JSON
        [ContentType.JSON] | ContentType.AVRO
    }
}
