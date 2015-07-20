package pl.allegro.tech.hermes.common.broker;

import com.google.common.base.Joiner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;

public class ZookeeperOffsets {

    private static final String OFFSET_PATTERN_PATH = "/consumers/%s/offsets/%s";

    public static String getPartitionOffsetPath(TopicName topicName, String subscriptionName, int partition) {
        return Joiner.on("/").join(getOffsetPath(topicName, subscriptionName), partition);
    }

    private static String getOffsetPath(TopicName topicName, String subscriptionName) {
        return String.format(OFFSET_PATTERN_PATH, Subscription.getId(topicName, subscriptionName), topicName.qualifiedName());
    }
}
