package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub

import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import org.apache.avro.Schema
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import spock.lang.Specification
import spock.lang.Subject

import java.nio.charset.StandardCharsets

class GooglePubSubMessageTransformerRawTest extends Specification {

    GooglePubSubMetadataAppender metadataAppender = new GooglePubSubMetadataAppender()

    @Subject
    GooglePubSubMessageTransformer pubSubMessages = new GooglePubSubMessageTransformerRaw(metadataAppender)

    def 'should convert standard message'() {
        given:
        Message msg = MessageBuilder.testMessage()

        when:
        PubsubMessage pubsubMessage = pubSubMessages.fromHermesMessage(msg)
        Map<String, String> attributes = pubsubMessage.getAttributesMap()

        then:
        pubsubMessage.getData() == ByteString.copyFrom(MessageBuilder.TEST_MESSAGE_CONTENT, StandardCharsets.UTF_8)
        attributes["tn"] == msg.getTopic()
        attributes["ts"] == msg.getPublishingTimestamp().toString()
        attributes["id"] == msg.getId()
    }

    def 'should convert avro message'() {
        given:
        Message msg = MessageBuilder.newBuilder()
                .withId("id123")
                .withTopic("topic123")
                .withContent("test", StandardCharsets.UTF_8)
                .withPublishingTimestamp(123L)
                .withSchema(Schema.create(Schema.Type.NULL), 7, 6)
                .withAdditionalHeaders([])
                .withExternalMetadata([:])
                .build()

        when:
        PubsubMessage pubsubMessage = pubSubMessages.fromHermesMessage(msg)
        Map<String, String> attributes = pubsubMessage.getAttributesMap()

        then:
        pubsubMessage.getData() == ByteString.copyFrom("test", StandardCharsets.UTF_8)
        attributes["tn"] == "topic123"
        attributes["id"] == "id123"
        attributes["ts"] == "123"
        attributes["sv"] == "6"
        attributes["sid"] == "7"
    }
}
