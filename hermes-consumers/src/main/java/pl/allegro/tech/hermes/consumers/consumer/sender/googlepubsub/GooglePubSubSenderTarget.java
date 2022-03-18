package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.pubsub.v1.TopicName;

import java.util.Objects;

class GooglePubSubSenderTarget {

    private final TopicName topicName;
    private final String pubSubEndpoint;

    private GooglePubSubSenderTarget(TopicName topicName, String pubSubEndpoint) {
        this.topicName = topicName;
        this.pubSubEndpoint = pubSubEndpoint;
    }

    TopicName getTopicName() {
        return topicName;
    }

    String getPubSubEndpoint() {
        return pubSubEndpoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GooglePubSubSenderTarget that = (GooglePubSubSenderTarget) o;
        return Objects.equals(topicName, that.topicName) && Objects.equals(pubSubEndpoint, that.pubSubEndpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicName, pubSubEndpoint);
    }

    static PubSubTargetBuilder builder() {
        return new PubSubTargetBuilder();
    }

    static final class PubSubTargetBuilder {
        private TopicName topicName;
        private String pubSubEndpoint;

        private PubSubTargetBuilder() {
        }

        PubSubTargetBuilder withTopicName(TopicName topicName) {
            this.topicName = topicName;
            return this;
        }

        PubSubTargetBuilder withPubSubEndpoint(String pubSubEndpoint) {
            this.pubSubEndpoint = pubSubEndpoint;
            return this;
        }

        GooglePubSubSenderTarget build() {
            return new GooglePubSubSenderTarget(topicName, pubSubEndpoint);
        }
    }
}
