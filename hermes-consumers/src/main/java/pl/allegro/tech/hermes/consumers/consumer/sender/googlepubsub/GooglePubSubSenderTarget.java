package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.pubsub.v1.TopicName;
import java.util.Objects;

class GooglePubSubSenderTarget {

  private final TopicName topicName;
  private final String pubSubEndpoint;
  private final CompressionCodec compressionCodec;

  private GooglePubSubSenderTarget(
      TopicName topicName, String pubSubEndpoint, CompressionCodec compressionCodec) {
    this.topicName = topicName;
    this.pubSubEndpoint = pubSubEndpoint;
    this.compressionCodec = compressionCodec;
  }

  TopicName getTopicName() {
    return topicName;
  }

  String getPubSubEndpoint() {
    return pubSubEndpoint;
  }

  CompressionCodec getCompressionCodec() {
    return compressionCodec;
  }

  boolean isCompressionRequested() {
    return compressionCodec != CompressionCodec.EMPTY;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return skipCompressionCodecEquals((GooglePubSubSenderTarget) o);
  }

  @Override
  public int hashCode() {
    return skipCompressionCodecHashCode();
  }

  private boolean skipCompressionCodecEquals(GooglePubSubSenderTarget that) {
    return Objects.equals(topicName, that.topicName)
        && Objects.equals(pubSubEndpoint, that.pubSubEndpoint);
  }

  private int skipCompressionCodecHashCode() {
    return Objects.hash(topicName, pubSubEndpoint);
  }

  static PubSubTargetBuilder builder() {
    return new PubSubTargetBuilder();
  }

  static final class PubSubTargetBuilder {
    private TopicName topicName;
    private String pubSubEndpoint;
    private CompressionCodec compressionCodec;

    private PubSubTargetBuilder() {}

    PubSubTargetBuilder withTopicName(TopicName topicName) {
      this.topicName = topicName;
      return this;
    }

    PubSubTargetBuilder withPubSubEndpoint(String pubSubEndpoint) {
      this.pubSubEndpoint = pubSubEndpoint;
      return this;
    }

    PubSubTargetBuilder withCompressionCodec(CompressionCodec compressionCodec) {
      this.compressionCodec = compressionCodec;
      return this;
    }

    GooglePubSubSenderTarget build() {
      return new GooglePubSubSenderTarget(topicName, pubSubEndpoint, compressionCodec);
    }
  }
}
