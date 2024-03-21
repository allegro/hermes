package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TopicAndSubscription {

    private final Topic topic;
    private final String subscription;

    public TopicAndSubscription(Topic topic, String subscription) {
        this.topic = topic;
        this.subscription = subscription;
    }

    @JsonIgnore
    public Topic getTopic() {
        return topic;
    }

    public String getTopicName() {
        return topic.getQualifiedName();
    }

    public String getSubscription() {
        return subscription;
    }
}
