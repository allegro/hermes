package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub

import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets


class GooglePubSubMessagesWithCompressionTest extends Specification {

    @Unroll
    def 'should compress Pubsub message'() {
        given:
        def codecFactory = CompressionCodecFactory.builder()
                .fromCodecName(codecName)
                .withCompressionLevel(compressionLevel)
                .build()
        def rawPubSubMessages = new GooglePubSubMessages(new GooglePubSubMetadataAppender())
        def deflateCompressor = new GooglePubSubMessageCompressor(codecFactory.get())

        def pubSubMessagesWithCompression = new GooglePubSubMessagesWithCompression(
                new GooglePubSubMetadataCompressionAppender(codecName),
                deflateCompressor,
                rawPubSubMessages
        )
        def msg = MessageBuilder.testMessage()

        when:
        PubsubMessage pubsubMessage = pubSubMessagesWithCompression.fromHermesMessage(msg)
        Map<String, String> attributes = pubsubMessage.getAttributesMap()

        then:
        ByteString.copyFrom(MessageBuilder.TEST_MESSAGE_CONTENT, StandardCharsets.UTF_8).toByteArray() ==
                deflateCompressor.decompress(pubsubMessage.getData().toByteArray())

        assert attributes["cn"] == codecName

        assert attributes["tn"] == msg.getTopic()
        assert attributes["ts"] == msg.getPublishingTimestamp().toString()
        assert attributes["id"] == msg.getId()

        msg.getExternalMetadata().forEach({k, v ->
            assert attributes[k] == v
        })
        msg.getAdditionalHeaders().forEach({
            assert attributes[it.name] == it.value
        })

        where:
        codecName       | compressionLevel
        'deflate'       | 'high'
        'bzip2'         | ''
        'zstandard'     | 'low'
    }

    def 'should switch to raw processor on compression error'() {
        given:
        def codecName = "empty"
        def rawPubSubMessages = new GooglePubSubMessages(new GooglePubSubMetadataAppender())

        def compressorStub = Stub(GooglePubSubMessageCompressor)
        compressorStub.compress(_ as byte[]) >> {
            throw new IOException("compressor test exception")
        }

        def pubSubMessagesWithCompression = new GooglePubSubMessagesWithCompression(
                new GooglePubSubMetadataCompressionAppender(codecName),
                compressorStub,
                rawPubSubMessages
        )
        def msg = MessageBuilder.testMessage()

        when:
        PubsubMessage pubsubMessage = pubSubMessagesWithCompression.fromHermesMessage(msg)
        Map<String, String> attributes = pubsubMessage.getAttributesMap()

        then:
        ByteString.copyFrom(MessageBuilder.TEST_MESSAGE_CONTENT, StandardCharsets.UTF_8).toByteArray() ==
                pubsubMessage.getData().toByteArray()

        assert !attributes.containsKey("cn")

        assert attributes["tn"] == msg.getTopic()
        assert attributes["ts"] == msg.getPublishingTimestamp().toString()
        assert attributes["id"] == msg.getId()

        msg.getExternalMetadata().forEach({k, v ->
            assert attributes[k] == v
        })
        msg.getAdditionalHeaders().forEach({
            assert attributes[it.name] == it.value
        })
    }
}
