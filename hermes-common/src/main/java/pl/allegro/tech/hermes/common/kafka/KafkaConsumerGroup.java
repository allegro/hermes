package pl.allegro.tech.hermes.common.kafka;

import java.util.Objects;
import java.util.Set;

public class KafkaConsumerGroup {

    private final String groupId;

    private final Set<KafkaConsumerGroupMember> members;

    public KafkaConsumerGroup(String groupId, Set<KafkaConsumerGroupMember> members) {
        this.groupId = groupId;
        this.members = members;
    }

    public String getGroupId() {
        return groupId;
    }

    public Set<KafkaConsumerGroupMember> getMembers() {
        return members;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaConsumerGroup that = (KafkaConsumerGroup) o;
        return Objects.equals(groupId, that.groupId) &&
                Objects.equals(members, that.members);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, members);
    }
}