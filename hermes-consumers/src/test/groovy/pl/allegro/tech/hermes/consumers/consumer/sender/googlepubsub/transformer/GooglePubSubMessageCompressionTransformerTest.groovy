package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer

import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer.compression.CompressionCodec
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer.compression.CompressionCodecFactory
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer.compression.GooglePubSubMessageCompressor
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets


class GooglePubSubMessageCompressionTransformerTest extends Specification {

    @Unroll
    def 'should compress Pubsub message'() {
        given:
        def codecFactory = CompressionCodecFactory.builder()
                .withCodec(codec)
                .withCompressionLevel(compressionLevel)
                .build()
        def rawPubSubMessages = new GooglePubSubMessageTransformer(new GooglePubSubMetadataAppender())
        def compressor = new GooglePubSubMessageCompressor(codecFactory.get())

        def pubSubMessagesWithCompression = new GooglePubSubMessageCompressionTransformer(
                new GooglePubSubMetadataCompressionAppender(codec),
                compressor,
                rawPubSubMessages,
                0L
        )
        def msg = MessageBuilder.testMessage()

        when:
        PubsubMessage pubsubMessage = pubSubMessagesWithCompression.fromHermesMessage(msg)
        Map<String, String> attributes = pubsubMessage.getAttributesMap()

        then:
        ByteString.copyFrom(MessageBuilder.TEST_MESSAGE_CONTENT, StandardCharsets.UTF_8).toByteArray() ==
                compressor.decompress(pubsubMessage.getData().toByteArray())

        assert attributes["cn"] == header

        assert attributes["tn"] == msg.getTopic()
        assert attributes["ts"] == msg.getPublishingTimestamp().toString()
        assert attributes["id"] == msg.getId()

        where:
        codec                      | header | compressionLevel
        CompressionCodec.DEFLATE   | 'df'   | 'high'
        CompressionCodec.BZIP2     | 'bz2'  | ''
        CompressionCodec.ZSTANDARD | 'zstd' | 'low'
    }

    def 'should switch to raw transformer when compression error'() {
        given:
        def rawPubSubMessages = new GooglePubSubMessageTransformer(new GooglePubSubMetadataAppender())

        def compressorStub = Stub(GooglePubSubMessageCompressor)
        compressorStub.compress(_ as byte[]) >> {
            throw new IOException("compressor test exception")
        }

        def pubSubMessagesWithCompression = new GooglePubSubMessageCompressionTransformer(
                new GooglePubSubMetadataCompressionAppender(CompressionCodec.EMPTY),
                compressorStub,
                rawPubSubMessages,
                0L
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
    }
}
