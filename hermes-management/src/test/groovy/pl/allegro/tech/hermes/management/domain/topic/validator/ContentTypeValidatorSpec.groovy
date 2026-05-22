package pl.allegro.tech.hermes.management.domain.topic.validator

import pl.allegro.tech.hermes.api.ContentType
import spock.lang.Specification
import spock.lang.Unroll

class ContentTypeValidatorSpec extends Specification {

    @Unroll
    def "content type #contentType in whitelist #whitelist should be valid"() {
        given:
        def validator = new ContentTypeValidator(whitelist as Set<ContentType>)

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
        def validator = new ContentTypeValidator(whitelist as Set<ContentType>)

        when:
        validator.check(contentType)

        then:
        def thrown = thrown(TopicValidationException)
        thrown.message == "Content type $contentType is not within allowed content types ${whitelist as Set}"

        where:
        whitelist          | contentType
        [ContentType.AVRO] | ContentType.JSON
        [ContentType.JSON] | ContentType.AVRO
    }
}
