package pl.allegro.tech.hermes.frontend.publishing.preview

import pl.allegro.tech.hermes.domain.topic.preview.MessagePreview
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage
import pl.allegro.tech.hermes.test.helper.avro.AvroUser
import spock.lang.Specification
import spock.lang.Subject

class MessagePreviewFactoryTest extends Specification {

    @Subject
    MessagePreviewFactory factory

    def "should truncate message preview if it is too large"() {
        given:
            factory = new MessagePreviewFactory(maxContentSize)
        when:
            MessagePreview preview = factory.create(new JsonMessage('message-id', new byte[messageSize], 0L, "partition-key"))
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

    def "should truncate message preview if it is too large after decoding to JSON"() {
        given:
            def avroUser = new AvroUser()
            def message = new AvroMessage('message-id', avroUser.asBytes(), 0L, avroUser.compiledSchema, null)
            factory = new MessagePreviewFactory(avroUser.asJson().length() - 1)
        when:
            MessagePreview preview = factory.create(message)
        then:
            preview.truncated
    }
}
