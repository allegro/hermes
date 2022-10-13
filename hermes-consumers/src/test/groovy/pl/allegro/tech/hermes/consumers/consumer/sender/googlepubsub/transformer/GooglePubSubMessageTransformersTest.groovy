package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer

import com.google.pubsub.v1.TopicName
import pl.allegro.tech.hermes.consumers.config.GooglePubSubCompressorProperties
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubSenderTarget
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer.compression.CompressionCodec
import spock.lang.Specification
import spock.lang.Unroll

class GooglePubSubMessageTransformersTest extends Specification {

    @Unroll
    def 'should create message transformer according to Hermes and subscription settings'() {
        given:
        def properties = new GooglePubSubCompressorProperties()
        properties.setEnabled(compressionEnabled)

        def target = GooglePubSubSenderTarget.builder()
                .withTopicName(TopicName.newBuilder().setProject("project").setTopic("topic").build())
                .withPubSubEndpoint("pubsub.endpoint")
                .withCompressionCodec(codec)
                .build();
        def transformers = new GooglePubSubMessageTransformers(new GooglePubSubMetadataAppender(), properties);

        when:
        def transformer = transformers.createMessageTransformer(target)

        then:
        transformer instanceof GooglePubSubMessageCompressionTransformer == shouldCompress

        where:
        codec                  | compressionEnabled | shouldCompress
        CompressionCodec.BZIP2 | true               | true
        CompressionCodec.EMPTY | true               | false
        CompressionCodec.BZIP2 | false              | false
        CompressionCodec.EMPTY | false              | false
    }
}
