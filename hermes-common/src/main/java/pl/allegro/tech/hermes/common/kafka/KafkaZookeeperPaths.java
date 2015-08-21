package pl.allegro.tech.hermes.common.kafka;

import com.google.common.base.Joiner;

public final class KafkaZookeeperPaths {

    private KafkaZookeeperPaths() {
    }

    public static String offsetsPath(ConsumerGroupId groupId, KafkaTopicName topicName) {
        return join(consumerGroupPath(groupId), "offsets", topicName.asString());
    }

    public static String ownersPath(ConsumerGroupId groupId, KafkaTopicName topicName) {
        return join(consumerGroupPath(groupId), "owners", topicName.asString());
    }

    public static String idsPath(ConsumerGroupId groupId) {
        return join(consumerGroupPath(groupId), "ids");
    }

    public static String partitionOffsetPath(ConsumerGroupId groupId, KafkaTopicName topic, int partition) {
        return join(offsetsPath(groupId, topic), partition);
    }

    public static String consumerGroupPath(ConsumerGroupId groupId) {
        return join("/consumers", groupId.asString());
    }

    private static String join(Object... parts) {
        return Joiner.on("/").join(parts);
    }

}
