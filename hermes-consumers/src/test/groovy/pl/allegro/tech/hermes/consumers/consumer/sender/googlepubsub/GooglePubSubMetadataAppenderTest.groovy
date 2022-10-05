package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub

import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import org.apache.avro.Schema
import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.Header
import pl.allegro.tech.hermes.consumers.config.GooglePubSubSenderProperties
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

    private GooglePubSubSenderProperties properties = new GooglePubSubSenderProperties()

    @Subject
    GooglePubSubMetadataAppender appender = new GooglePubSubMetadataAppender(properties)

    void setup() {
        properties.includeMoreAttributes = true
    }

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

    def 'should add additional headers and external metadata to the message'() {
        Message message = messageWithAdditionalMetadata()

        when:
        PubsubMessage enrichedMessage = appender.append(pubSubMessage, message)

        then:
        enrichedMessage.getAttributesOrThrow("additional-header1") == "additional-header-value1"
        enrichedMessage.getAttributesOrThrow("additional-header2") == "additional-header-value2"
        enrichedMessage.getAttributesOrThrow("externalMetadata1") == "external-metadata-value1"
        enrichedMessage.getAttributesOrThrow("externalMetadata2") == "external-metadata-value2"
    }

    def 'should prefer basic headers over additional or external metadata with the same name'() {
        Message message = MessageBuilder.newBuilder()
                .withTopic(TOPIC_NAME)
                .withId(MESSAGE_ID)
                .withPublishingTimestamp(PUBLISHING_TIMESTAMP)
                .withContentType(ContentType.AVRO)
                .withSchema(Schema.create(Schema.Type.NULL), SCHEMA_ID, SCHEMA_VERSION)
                .withAdditionalHeaders([
                        new Header(GooglePubSubMetadataAppender.HEADER_NAME_TOPIC_NAME, "test"),
                        new Header(GooglePubSubMetadataAppender.HEADER_NAME_TIMESTAMP, "test"),
                        new Header(GooglePubSubMetadataAppender.HEADER_NAME_MESSAGE_ID, "test"),
                        new Header(GooglePubSubMetadataAppender.HEADER_NAME_SCHEMA_VERSION, "test"),
                        new Header(GooglePubSubMetadataAppender.HEADER_NAME_SCHEMA_ID, "test"),
                        new Header(GooglePubSubMetadataAppender.HEADER_NAME_SUBSCRIPTION_NAME, "test"),
                ])
                .withExternalMetadata([tn: "test", ts: "test", id: "test", sv: "test", sid: "test", sn: "test"])
                .withHasSubscriptionIdentityHeaders(true)
                .withSubscription(SUBSCRIPTION_NAME)
                .build()

        when:
        PubsubMessage enrichedMessage = appender.append(pubSubMessage, message)

        then:
        enrichedMessage.getAttributesOrThrow(GooglePubSubMetadataAppender.HEADER_NAME_TOPIC_NAME) == TOPIC_NAME
        enrichedMessage.getAttributesOrThrow(GooglePubSubMetadataAppender.HEADER_NAME_MESSAGE_ID) == MESSAGE_ID
        enrichedMessage.getAttributesOrThrow(GooglePubSubMetadataAppender.HEADER_NAME_TIMESTAMP) == PUBLISHING_TIMESTAMP.toString()
        enrichedMessage.getAttributesOrThrow(GooglePubSubMetadataAppender.HEADER_NAME_SUBSCRIPTION_NAME) == SUBSCRIPTION_NAME
        enrichedMessage.getAttributesOrThrow(GooglePubSubMetadataAppender.HEADER_NAME_SCHEMA_ID) == SCHEMA_ID.toString()
        enrichedMessage.getAttributesOrThrow(GooglePubSubMetadataAppender.HEADER_NAME_SCHEMA_VERSION) == SCHEMA_VERSION.toString()
    }

    def 'should not add additional and external headers where includeMoreAttributes property is false'() {
        given:
        GooglePubSubSenderProperties properties = new GooglePubSubSenderProperties()
        properties.includeMoreAttributes = false
        GooglePubSubMetadataAppender appenderUnderTest = new GooglePubSubMetadataAppender(properties)

        when:
        PubsubMessage enrichedMessage = appenderUnderTest.append(pubSubMessage, messageWithAdditionalMetadata())

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
