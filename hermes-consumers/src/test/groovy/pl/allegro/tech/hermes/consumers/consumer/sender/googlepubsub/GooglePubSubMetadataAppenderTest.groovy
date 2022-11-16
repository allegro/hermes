package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub

import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import org.apache.avro.Schema
import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.Header
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.nio.charset.StandardCharsets

class GooglePubSubMetadataAppenderTest extends Specification {

    private static final Long PUBLISHING_TIMESTAMP = 1647199023000L
    private static final String TOPIC_NAME = "topic-name"
    private static final String MESSAGE_ID = "id"
    private static final String SUBSCRIPTION_NAME = "subscription"
    private static final int SCHEMA_ID = 7
    private static final int SCHEMA_VERSION = 3

    PubsubMessage pubSubMessage = PubsubMessage.newBuilder()
            .setData(ByteString.copyFrom("test", StandardCharsets.UTF_8))
            .build()

    @Subject
    GooglePubSubMetadataAppender appender = new GooglePubSubMetadataAppender()

    @Unroll
    def 'should add all basic headers for json message (with subscription identity headers: #hasSubscriptionIdentityHeaders)'(boolean hasSubscriptionIdentityHeaders) {
        given:
        Message message = MessageBuilder.newBuilder()
                .withTopic(TOPIC_NAME)
                .withId(MESSAGE_ID)
                .withPublishingTimestamp(PUBLISHING_TIMESTAMP)
                .withContentType(ContentType.JSON)
                .withAdditionalHeaders([])
                .withExternalMetadata([:])
                .withHasSubscriptionIdentityHeaders(hasSubscriptionIdentityHeaders)
                .withSubscription(SUBSCRIPTION_NAME)
                .build()

        when:
        PubsubMessage enrichedMessage = appender.append(pubSubMessage, message)

        then:
        enrichedMessage.getAttributesOrThrow(GooglePubSubMetadataAppender.HEADER_NAME_TOPIC_NAME) == TOPIC_NAME
        enrichedMessage.getAttributesOrThrow(GooglePubSubMetadataAppender.HEADER_NAME_MESSAGE_ID) == MESSAGE_ID
        enrichedMessage.getAttributesOrThrow(GooglePubSubMetadataAppender.HEADER_NAME_TIMESTAMP) == PUBLISHING_TIMESTAMP.toString()
        if (hasSubscriptionIdentityHeaders) {
            enrichedMessage.getAttributesOrThrow(GooglePubSubMetadataAppender.HEADER_NAME_SUBSCRIPTION_NAME) == SUBSCRIPTION_NAME
        }

        where:
        hasSubscriptionIdentityHeaders << [true, false]
    }

    def 'should add avro-related headers for the avro message'() {
        given:
        Message message = MessageBuilder.newBuilder()
                .withTopic(TOPIC_NAME)
                .withId(MESSAGE_ID)
                .withPublishingTimestamp(PUBLISHING_TIMESTAMP)
                .withContentType(ContentType.AVRO)
                .withSchema(Schema.create(Schema.Type.NULL), SCHEMA_ID, SCHEMA_VERSION)
                .withAdditionalHeaders([])
                .withExternalMetadata([:])
                .build()

        when:
        PubsubMessage enrichedMessage = appender.append(pubSubMessage, message)

        then:
        enrichedMessage.getAttributesOrThrow(GooglePubSubMetadataAppender.HEADER_NAME_SCHEMA_ID) == SCHEMA_ID.toString()
        enrichedMessage.getAttributesOrThrow(GooglePubSubMetadataAppender.HEADER_NAME_SCHEMA_VERSION) == SCHEMA_VERSION.toString()
    }

    def 'should not add additional headers and external metadata to the message'() {
        Message message = messageWithAdditionalMetadata()

        when:
        PubsubMessage enrichedMessage = appender.append(pubSubMessage, message)

        then:
        !enrichedMessage.containsAttributes("additional-header1")
        !enrichedMessage.containsAttributes("additional-header2")
        !enrichedMessage.containsAttributes("externalMetadata1")
        !enrichedMessage.containsAttributes("externalMetadata2")
    }

    private static Message messageWithAdditionalMetadata() {
        Message message = MessageBuilder.newBuilder()
                .withTopic(TOPIC_NAME)
                .withId(MESSAGE_ID)
                .withPublishingTimestamp(PUBLISHING_TIMESTAMP)
                .withContentType(ContentType.JSON)
                .withAdditionalHeaders([
                        new Header("additional-header1", "additional-header-value1"),
                        new Header("additional-header2", "additional-header-value2")])
                .withExternalMetadata([
                        externalMetadata1: "external-metadata-value1",
                        externalMetadata2: "external-metadata-value2"])
                .withSubscription(SUBSCRIPTION_NAME)
                .build()
        message
    }
}
