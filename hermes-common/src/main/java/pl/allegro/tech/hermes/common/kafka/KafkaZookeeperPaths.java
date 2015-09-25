package pl.allegro.tech.hermes.common.kafka;

import com.google.common.base.Joiner;

public final class KafkaZookeeperPaths {

    private KafkaZookeeperPaths() {
    }

    public static String offsetsPath(ConsumerGroupId groupId, KafkaTopic topicName) {
        return join(consumerGroupPath(groupId), "offsets", topicName.name());
    }

    public static String ownersPath(ConsumerGroupId groupId, KafkaTopic topicName) {
        return join(consumerGroupPath(groupId), "owners", topicName.name());
    }

    public static String idsPath(ConsumerGroupId groupId) {
        return join(consumerGroupPath(groupId), "ids");
    }

    public static String partitionOffsetPath(ConsumerGroupId groupId, KafkaTopic topic, int partition) {
        return join(offsetsPath(groupId, topic), partition);
    }

    public static String consumerGroupPath(ConsumerGroupId groupId) {
        return join("/consumers", groupId.asString());
    }

    public static String topicPath(KafkaTopic topicName) {
        return join("/brokers/topics", topicName.name());
    }

    private static String join(Object... parts) {
        return Joiner.on("/").join(parts);
    }

}
