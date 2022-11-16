package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub

import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets


class GooglePubSubMessageTransformerCompressionTest extends Specification {

    @Unroll
    def 'should compress PubSub message'() {
        given:
        def compressor = new MessageCompressor(CompressionCodecFactory.of(codec, compressionLevel))

        def pubSubMessagesWithCompression = new GooglePubSubMessageTransformerCompression(
                new GooglePubSubMetadataCompressionAppender(codec), compressor)

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
        CompressionCodec.DEFLATE   | 'df'   | CompressionCodecFactory.CompressionLevel.HIGH
        CompressionCodec.BZIP2     | 'bz2'  | CompressionCodecFactory.CompressionLevel.MEDIUM
        CompressionCodec.ZSTANDARD | 'zstd' | CompressionCodecFactory.CompressionLevel.LOW
    }

    def 'should throw exception on compression error'() {
        given:
        def compressorStub = Stub(MessageCompressor)
        compressorStub.compress(_ as byte[]) >> {
            throw new IOException("compressor test exception")
        }
        def pubSubMessagesWithCompression = new GooglePubSubMessageTransformerCompression(
                new GooglePubSubMetadataCompressionAppender(CompressionCodec.EMPTY), compressorStub)

        when:
        pubSubMessagesWithCompression.fromHermesMessage(MessageBuilder.testMessage())

        then:
        thrown(GooglePubSubMessageCompressionException)
    }
}
