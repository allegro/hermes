package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub

import com.google.pubsub.v1.TopicName
import spock.lang.Specification
import spock.lang.Unroll

class GooglePubSubMessageTransformerProviderTest extends Specification {

    @Unroll
    def 'should create message transformer according to Hermes and subscription settings'() {
        given:
        def creator = GooglePubSubMessageTransformerCreator.creator()
                .withCompressionEnabled(compressionEnabled)
                .withCompressionLevel("MEDIUM")
                .withCompressionThresholdBytes(1)

        def target = GooglePubSubSenderTarget.builder()
                .withTopicName(TopicName.newBuilder().setProject("project").setTopic("topic").build())
                .withPubSubEndpoint("pubsub.endpoint")
                .withCompressionCodec(codec)
                .build();

        def transformers = new GooglePubSubMessageTransformerProvider(creator)

        when:
        def transformer = transformers.getTransformerForTargetEndpoint(target)

        then:
        transformer instanceof GooglePubSubMessageTransformerOptionalCompression == shouldCompress
        transformer instanceof GooglePubSubMessageTransformerRaw != shouldCompress

        where:
        codec                  | compressionEnabled | shouldCompress
        CompressionCodec.BZIP2 | true               | true
        CompressionCodec.EMPTY | true               | false
        CompressionCodec.BZIP2 | false              | false
        CompressionCodec.EMPTY | false              | false
    }
}
