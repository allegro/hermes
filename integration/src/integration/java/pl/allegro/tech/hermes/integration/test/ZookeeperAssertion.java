package pl.allegro.tech.hermes.integration.test;

import org.apache.curator.framework.CuratorFramework;
import org.assertj.core.api.AbstractAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;

import static org.assertj.core.api.Assertions.assertThat;

public class ZookeeperAssertion extends AbstractAssert<ZookeeperAssertion, CuratorFramework> {

    public static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperAssertion.class);

    public ZookeeperAssertion(CuratorFramework actual, Class<?> selfType) {
        super(actual, selfType);
    }

    public void offsetsAreNotRetracted(String group, String topic, String subscription, int partitions, int offset) {
        for (int i = 0; i < 200; i++) {
            try {
                for (int j = 0; j < partitions; j++) {
                    assertThat(offsetValue(group, topic, subscription, j)).isEqualTo(offset);
                }
                Thread.sleep(10);
            } catch (Exception exception) {
                LOGGER.warn("Something went wrong.", exception);
            }
        }
    }

    private long offsetValue(String group, String topic, String subscription, int partition) throws Exception {
        return Long.valueOf(new String(actual.getData().forPath(subscriptionOffsetPath(group, topic, subscription, partition))));
    }

    private String subscriptionOffsetPath(String group, String topic, String subscription, int partition) {
        TopicName topicName = new TopicName(group, topic);
        return "/consumers/" + Subscription.getId(topicName, subscription) + "/offsets/" + topicName.qualifiedName() + "/" + partition;
    }
}
