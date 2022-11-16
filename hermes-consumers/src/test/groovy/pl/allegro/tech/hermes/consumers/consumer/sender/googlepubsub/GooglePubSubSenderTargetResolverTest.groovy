package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub

import com.google.pubsub.v1.TopicName
import pl.allegro.tech.hermes.api.EndpointAddress
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class GooglePubSubSenderTargetResolverTest extends Specification {

    @Subject
    GooglePubSubSenderTargetResolver resolver = new GooglePubSubSenderTargetResolver()

    @Unroll
    def 'should resolve endpoint address'() {
        given:
        EndpointAddress endpointAddress = new EndpointAddress(endpointAddressUri)

        when:
        GooglePubSubSenderTarget senderTarget = resolver.resolve(endpointAddress)

        then:
        senderTarget.pubSubEndpoint == "pubsub.googleapis.com:443"
        senderTarget.topicName == TopicName.of("test-project", "test-topic")
        senderTarget.getCompressionCodec() == expectedCodec

        where:
        endpointAddressUri                                                                                       | expectedCodec
        "googlepubsub://pubsub.googleapis.com:443/projects/test-project/topics/test-topic"                       | CompressionCodec.EMPTY
        "googlepubsub://pubsub.googleapis.com:443/projects/test-project/topics/test-topic?"                      | CompressionCodec.EMPTY
        "googlepubsub://pubsub.googleapis.com:443/projects/test-project/topics/test-topic?compression"           | CompressionCodec.EMPTY
        "googlepubsub://pubsub.googleapis.com:443/projects/test-project/topics/test-topic?compression="          | CompressionCodec.EMPTY
        "googlepubsub://pubsub.googleapis.com:443/projects/test-project/topics/test-topic?compression=bzip2"     | CompressionCodec.BZIP2
        "googlepubsub://pubsub.googleapis.com:443/projects/test-project/topics/test-topic?compression=deflate"   | CompressionCodec.DEFLATE
        "googlepubsub://pubsub.googleapis.com:443/projects/test-project/topics/test-topic?compression=zstandard" | CompressionCodec.ZSTANDARD
    }

    @Unroll
    def 'should throw IllegalArgumentException when the endpoint address is #endpointAddressUri'(String endpointAddressUri) {
        given:
        EndpointAddress endpointAddress = new EndpointAddress(endpointAddressUri)

        when:
        resolver.resolve(endpointAddress)

        then:
        thrown(IllegalArgumentException)

        where:
        endpointAddressUri << [
                "pubsub://pubsub.googleapis.com:443/projects/test-project/topics/test-topic",
                "pubsub.googleapis.com:443/projects/test-project/topics/test-topic",
                "googlepubsub://pubsub.googleapis.com/projects/test-project/topics/test-topic",
                "googlepubsub://projects/test-project/topics/test-topic",
                "projects/test-project/topics/test-topic",
                "googlepubsub://pubsub.googleapis.com:443/projects/test-project/topic/test-topic",
                "googlepubsub://pubsub.googleapis.com:443/project/test-project/topics/test-topic",
                "test-topic",
                "googlepubsub://pubsub.googleapis.com:443/projects/test-project/topics/test-topic?compression=unsupported"
        ]
    }
}
