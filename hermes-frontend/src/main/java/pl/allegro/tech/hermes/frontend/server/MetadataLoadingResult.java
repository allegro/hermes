package pl.allegro.tech.hermes.frontend.server;

import pl.allegro.tech.hermes.api.TopicName;

public final class MetadataLoadingResult {

    enum Type { SUCCESS, FAILURE }

    private final Type type;

    private final TopicName topicName;

    MetadataLoadingResult(Type type, TopicName topicName) {
        this.type = type;
        this.topicName = topicName;
    }

    static MetadataLoadingResult success(TopicName topicName) {
        return new MetadataLoadingResult(Type.SUCCESS, topicName);
    }

    static MetadataLoadingResult failure(TopicName topicName) {
        return new MetadataLoadingResult(Type.FAILURE, topicName);
    }

    Type getType() {
        return type;
    }

    TopicName getTopicName() {
        return topicName;
    }

    public boolean isFailure() {
        return Type.FAILURE == type;
    }
}
