package pl.allegro.tech.hermes.api;

import static com.google.common.base.Preconditions.checkArgument;

public class SubscriptionName {

    private String name;
    private TopicName topicName;

    private SubscriptionName() {
    }

    public SubscriptionName(String name, TopicName topicName) {
        this.name = name;
        this.topicName = topicName;
    }

    public String getName() {
        return name;
    }

    public TopicName getTopicName() {
        return topicName;
    }

    public static SubscriptionName fromString(String string) {
        String[] tokens = string.split("\\$");
        checkArgument(tokens.length > 1, "Incorrect string format. Expected 'topic$subscription'. Found:'%s'", string);
        return new SubscriptionName(tokens[1], TopicName.fromQualifiedName(tokens[0]));
    }

    @Override
    public String toString() {
        return topicName.qualifiedName() + "$" + name;
    }
}
