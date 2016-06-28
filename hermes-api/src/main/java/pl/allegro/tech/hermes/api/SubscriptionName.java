package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static pl.allegro.tech.hermes.api.helpers.Replacer.replaceInAll;

public class SubscriptionName {

    private final String name;
    private final TopicName topicName;
    private final String id;

    @JsonCreator
    public SubscriptionName(@JsonProperty("name") String name, @JsonProperty("topicName") TopicName topicName) {
        this.name = name;
        this.topicName = topicName;
        this.id = createId();
    }

    private String createId() {
        return Joiner.on("_").join(replaceInAll("_", "__", topicName.getGroupName(), topicName.getName(), name));
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

    @JsonIgnore
    public String getId() {
        return Subscription.getId(getTopicName(), getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionName that = (SubscriptionName) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(topicName, that.topicName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, topicName);
    }
}
