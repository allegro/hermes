package pl.allegro.tech.hermes.consumers.consumer.sender.pubsub;

import com.google.common.base.Objects;
import com.google.pubsub.v1.TopicName;

public class PubSubSenderTarget {

    private final TopicName topicName;
    private final String pubSubEndpoint;

    private PubSubSenderTarget(TopicName topicName, String pubSubEndpoint) {
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
        PubSubSenderTarget that = (PubSubSenderTarget) o;
        return Objects.equal(topicName, that.topicName) && Objects.equal(pubSubEndpoint, that.pubSubEndpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(topicName, pubSubEndpoint);
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

        public PubSubSenderTarget build() {
            return new PubSubSenderTarget(topicName, pubSubEndpoint);
        }
    }
}
