package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub

import com.google.api.gax.batching.BatchingSettings
import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.ExecutorProvider
import com.google.api.gax.retrying.RetrySettings
import com.google.api.gax.rpc.TransportChannelProvider
import com.google.cloud.pubsub.v1.Publisher
import com.google.pubsub.v1.TopicName
import spock.lang.Specification

class GooglePubSubClientsPoolTest extends Specification {

    class GooglePubSubClientsPoolUnderTest extends GooglePubSubClientsPool {

        private Publisher publisher

        GooglePubSubClientsPoolUnderTest(CredentialsProvider credentialsProvider,
                                         ExecutorProvider publishingExecutorProvider,
                                         RetrySettings retrySettings,
                                         BatchingSettings batchingSettings,
                                         TransportChannelProvider transportChannelProvider,
                                         Publisher publisher) {
            super(credentialsProvider, publishingExecutorProvider, retrySettings, batchingSettings, transportChannelProvider)
            this.publisher = publisher
        }

        @Override
        protected GooglePubSubClient createClient(GooglePubSubSenderTarget resolvedTarget) throws IOException {
            return new GooglePubSubClient(this.publisher)
        }
    }

    def 'should return the same PubSub client for both compressing and raw message transformer'() {
        given:
        def topic = TopicName.of("project","pl.allegro.topic")
        def pubSubEndpoint = "https://pubsub.endpoint"

        def poolUnderTest = new GooglePubSubClientsPoolUnderTest(Stub(CredentialsProvider), Stub(ExecutorProvider),
                Stub(RetrySettings), Stub(BatchingSettings), Stub(TransportChannelProvider), Stub(Publisher))

        def targetWithCodec = GooglePubSubSenderTarget.builder()
                .withTopicName(topic)
                .withPubSubEndpoint(pubSubEndpoint)
                .withCompressionCodec(CompressionCodec.BZIP2)
                .build()

        def targetWithoutCodec = GooglePubSubSenderTarget.builder()
                .withTopicName(topic)
                .withPubSubEndpoint(pubSubEndpoint)
                .build()

        when:
        def compressingClient = poolUnderTest.acquire(targetWithCodec)
        def rawClient = poolUnderTest.acquire(targetWithoutCodec)

        then:
        compressingClient == rawClient
    }
}
