package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub

import com.google.pubsub.v1.PubsubMessage
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import spock.lang.Specification
import spock.lang.Unroll


class GooglePubSubMessageTransformerOptionalCompressionTest extends Specification {

    @Unroll
    def 'should compress or not according to message size'() {
        given:
        def transformerUnderTest = GooglePubSubMessageTransformerCreator.creator()
                .withCompressionEnabled(true)
                .withCompressionLevel("MEDIUM")
                .withCompressionThresholdBytes(compressionThresholdBytes)
                .transformerForCodec(CompressionCodec.BZIP2)
                .get()

        def msg = MessageBuilder.testMessage()

        when:
        PubsubMessage pubsubMessage = transformerUnderTest.fromHermesMessage(msg)
        Map<String, String> attributes = pubsubMessage.getAttributesMap()

        then:
        assert (attributes["cn"] != null) == shouldCompress

        where:
        compressionThresholdBytes | shouldCompress
        1                         | true
        1000                      | false
    }
}
