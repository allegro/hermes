package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.pubsub.v1.TopicName;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer.compression.CompressionCodec;

import java.util.Objects;

public class GooglePubSubSenderTarget {

    private final TopicName topicName;
    private final String pubSubEndpoint;
    private final CompressionCodec compressionCodec;

    private GooglePubSubSenderTarget(TopicName topicName, String pubSubEndpoint, CompressionCodec compressionCodec) {
        this.topicName = topicName;
        this.pubSubEndpoint = pubSubEndpoint;
        this.compressionCodec = compressionCodec;
    }

    public TopicName getTopicName() {
        return topicName;
    }

    public String getPubSubEndpoint() {
        return pubSubEndpoint;
    }

    public CompressionCodec getCompressionCodec() {
        return compressionCodec;
    }

    public boolean isCompressionRequested() {
        return compressionCodec != CompressionCodec.EMPTY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GooglePubSubSenderTarget that = (GooglePubSubSenderTarget) o;
        return Objects.equals(topicName, that.topicName) && Objects.equals(pubSubEndpoint, that.pubSubEndpoint) &&
                Objects.equals(compressionCodec, that.compressionCodec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicName, pubSubEndpoint, compressionCodec);
    }

    public static PubSubTargetBuilder builder() {
        return new PubSubTargetBuilder();
    }

    public static final class PubSubTargetBuilder {
        private TopicName topicName;
        private String pubSubEndpoint;
        private CompressionCodec compressionCodec;

        private PubSubTargetBuilder() {
        }

        public PubSubTargetBuilder withTopicName(TopicName topicName) {
            this.topicName = topicName;
            return this;
        }

        public PubSubTargetBuilder withPubSubEndpoint(String pubSubEndpoint) {
            this.pubSubEndpoint = pubSubEndpoint;
            return this;
        }

        public PubSubTargetBuilder withCompressionCodec(CompressionCodec compressionCodec) {
            this.compressionCodec = compressionCodec;
            return this;
        }

        public GooglePubSubSenderTarget build() {
            return new GooglePubSubSenderTarget(topicName, pubSubEndpoint, compressionCodec);
        }
    }
}
