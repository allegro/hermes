package pl.allegro.tech.hermes.frontend.publishing.preview

import pl.allegro.tech.hermes.domain.topic.preview.MessagePreview
import spock.lang.Specification
import spock.lang.Subject

class MessagePreviewFactoryTest extends Specification {

    @Subject
    MessagePreviewFactory factory

    def "should truncate message preview if it is too large"() {
        given:
            factory = new MessagePreviewFactory(maxContentSize)
        when:
            MessagePreview preview = factory.create(new byte[messageSize])
        then:
            preview.truncated == shouldTruncate
        where:
            messageSize | maxContentSize || shouldTruncate
            50          | 100            || false
            99          | 100            || false
            100         | 100            || false
            101         | 100            || true
            150         | 100            || true
    }
}
