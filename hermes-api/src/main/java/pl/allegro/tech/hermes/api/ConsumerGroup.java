package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Set;

public class ConsumerGroup {

  private final String clusterName;
  private final String groupId;
  private final String state;

  private final Set<ConsumerGroupMember> members;

  @JsonCreator
  public ConsumerGroup(
      @JsonProperty("clusterName") String clusterName,
      @JsonProperty("groupId") String groupId,
      @JsonProperty("state") String state,
      @JsonProperty("members") Set<ConsumerGroupMember> members) {
    this.clusterName = clusterName;
    this.groupId = groupId;
    this.state = state;
    this.members = members;
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getState() {
    return state;
  }

  public Set<ConsumerGroupMember> getMembers() {
    return members;
  }

  public boolean isStable() {
    return state.equals("Stable");
  }

  public boolean isEmpty() {
    return state.equals("Empty");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsumerGroup that = (ConsumerGroup) o;
    return Objects.equals(clusterName, that.clusterName)
        && Objects.equals(groupId, that.groupId)
        && Objects.equals(state, that.state)
        && Objects.equals(members, that.members);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterName, groupId, state, members);
  }
}
