package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub

import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets


class GooglePubSubMessageTransformerCompressionTest extends Specification {

    def rawTransformer = new GooglePubSubMessageTransformerRaw(new GooglePubSubMetadataAppender())

    @Unroll
    def 'should compress PubSub message'() {
        given:
        def alwaysCompressThresholdBytes = 1
        def compressor = new MessageCompressor(CompressionCodecFactory.of(codec, compressionLevel))
        def transformerUnderTest = new GooglePubSubMessageTransformerCompression(
                alwaysCompressThresholdBytes,
                rawTransformer,
                new GooglePubSubMetadataCompressionAppender(codec),
                compressor
        )
        def msg = MessageBuilder.testMessage()

        when:
        PubsubMessage pubsubMessage = transformerUnderTest.fromHermesMessage(msg)
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
        CompressionCodec.DEFLATE   | 'df'   | CompressionCodecFactory.CompressionLevel.HIGH
        CompressionCodec.BZIP2     | 'bz2'  | CompressionCodecFactory.CompressionLevel.MEDIUM
        CompressionCodec.ZSTANDARD | 'zstd' | CompressionCodecFactory.CompressionLevel.LOW
    }

    def 'should throw exception on compression error'() {
        given:
        def failingCompressorStub = Stub(MessageCompressor)
        failingCompressorStub.compress(_ as byte[]) >> {
            throw new IOException("compressor test exception")
        }
        def alwaysCompressThresholdBytes = 1
        def pubSubMessagesWithCompression = new GooglePubSubMessageTransformerCompression(
                alwaysCompressThresholdBytes,
                rawTransformer,
                new GooglePubSubMetadataCompressionAppender(CompressionCodec.BZIP2),
                failingCompressorStub
        )

        when:
        pubSubMessagesWithCompression.fromHermesMessage(MessageBuilder.testMessage())

        then:
        thrown(GooglePubSubMessageCompressionException)
    }

    @Unroll
    def 'should compress or not according to message size'() {
        given:
        def compressor = new MessageCompressor(CompressionCodecFactory
                .of(CompressionCodec.BZIP2, CompressionCodecFactory.CompressionLevel.MEDIUM))
        def transformerUnderTest = new GooglePubSubMessageTransformerCompression(
                compressionThresholdBytes,
                rawTransformer,
                new GooglePubSubMetadataCompressionAppender(CompressionCodec.BZIP2),
                compressor
        )
        def msg = MessageBuilder.testMessage()

        when:
        PubsubMessage pubsubMessage = transformerUnderTest.fromHermesMessage(msg)
        Map<String, String> attributes = pubsubMessage.getAttributesMap()

        then:
        assert (attributes["cn"] != null) == shouldCompress

        where:
        compressionThresholdBytes || shouldCompress
        1                         || true
        1000                      || false
    }
}
