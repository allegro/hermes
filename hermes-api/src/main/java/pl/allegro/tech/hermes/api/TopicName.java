package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Pattern;

import java.util.Objects;

import static pl.allegro.tech.hermes.api.constraints.Names.ALLOWED_NAME_REGEX;

public class TopicName {

    public static final char GROUP_SEPARATOR = '.';

    @NotEmpty @Pattern(regexp = ALLOWED_NAME_REGEX)
    private final String groupName;

    @NotEmpty @Pattern(regexp = ALLOWED_NAME_REGEX)
    private final String name;

    @JsonCreator
    public TopicName(@JsonProperty("groupName") String groupName, @JsonProperty("name") String name) {
        this.groupName = groupName;
        this.name = name;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getName() {
        return name;
    }

    public String qualifiedName() {
        return groupName + GROUP_SEPARATOR + name;
    }

    public static String toQualifiedName(TopicName topicName) {
        return topicName != null ? topicName.qualifiedName() : null;
    }

    public static TopicName fromQualifiedName(String qualifiedName) {
        if (Strings.isNullOrEmpty(qualifiedName)) {
            return null;
        }

        int index = qualifiedName.lastIndexOf(GROUP_SEPARATOR);
        if (index == -1) {
            throw new IllegalArgumentException("Missing group");
        }

        String groupName = qualifiedName.substring(0, index);
        String topicName = qualifiedName.substring(index + 1, qualifiedName.length());
        return new TopicName(groupName, topicName);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("groupName", groupName)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TopicName that = (TopicName) o;

        return Objects.equals(this.name, that.name)
                && Objects.equals(this.groupName, that.groupName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, groupName);
    }
}
