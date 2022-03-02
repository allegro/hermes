package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.pubsub.v1.TopicName;

import java.util.Objects;

public class GooglePubSubSenderTarget {

    private final TopicName topicName;
    private final String pubSubEndpoint;

    private GooglePubSubSenderTarget(TopicName topicName, String pubSubEndpoint) {
        this.topicName = topicName;
        this.pubSubEndpoint = pubSubEndpoint;
    }

    public TopicName getTopicName() {
        return topicName;
    }

    public String getPubSubEndpoint() {
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

    public static PubSubTargetBuilder builder() {
        return new PubSubTargetBuilder();
    }


    public static final class PubSubTargetBuilder {
        private TopicName topicName;
        private String pubSubEndpoint;

        private PubSubTargetBuilder() {
        }

        public static PubSubTargetBuilder aPubSubTarget() {
            return new PubSubTargetBuilder();
        }

        public PubSubTargetBuilder withTopicName(TopicName topicName) {
            this.topicName = topicName;
            return this;
        }

        public PubSubTargetBuilder withPubSubEndpoint(String pubSubEndpoint) {
            this.pubSubEndpoint = pubSubEndpoint;
            return this;
        }

        public GooglePubSubSenderTarget build() {
            return new GooglePubSubSenderTarget(topicName, pubSubEndpoint);
        }
    }
}
