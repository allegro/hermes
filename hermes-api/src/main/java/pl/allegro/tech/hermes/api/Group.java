package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

public class Group {

  @NotNull private final String groupName;

  @JsonCreator
  public Group(@JsonProperty("groupName") String groupName) {
    this.groupName = groupName;
  }

  public static Group from(String groupName) {
    return new Group(groupName);
  }

  public String getGroupName() {
    return groupName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Group)) {
      return false;
    }
    Group group = (Group) o;

    return Objects.equals(this.getGroupName(), group.getGroupName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupName);
  }

  @Override
  public String toString() {
    return "Group(" + groupName + ")";
  }
}
