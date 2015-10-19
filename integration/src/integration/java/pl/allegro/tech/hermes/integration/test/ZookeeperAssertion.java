package pl.allegro.tech.hermes.integration.test;

import org.apache.curator.framework.CuratorFramework;
import org.assertj.core.api.AbstractAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaZookeeperPaths;

import static org.assertj.core.api.Assertions.assertThat;

public class ZookeeperAssertion extends AbstractAssert<ZookeeperAssertion, CuratorFramework> {

    public static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperAssertion.class);
    private final KafkaNamesMapper kafkaNamesMapper;

    public ZookeeperAssertion(CuratorFramework actual, Class<?> selfType, KafkaNamesMapper kafkaNamesMapper) {
        super(actual, selfType);
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    public void offsetsAreNotRetractedOnPrimaryKafkaTopic(Topic topic, String subscription, int partitions, int offset) {
        ConsumerGroupId kafkaGroupId = kafkaNamesMapper.toConsumerGroupId(Subscription.getId(topic.getName(), subscription));
        KafkaTopicName kafkaTopicName = kafkaNamesMapper.toKafkaTopics(topic).getPrimary().name();

        for (int i = 0; i < 200; i++) {
            try {
                for (int j = 0; j < partitions; j++) {
                    assertThat(offsetValue(kafkaGroupId, kafkaTopicName, j)).isEqualTo(offset);
                }
                Thread.sleep(10);
            } catch (Exception exception) {
                LOGGER.warn("Something went wrong.", exception);
            }
        }
    }

    private long offsetValue(ConsumerGroupId groupId, KafkaTopicName topic, int partition) throws Exception {
        String offsetPath = KafkaZookeeperPaths.partitionOffsetPath(groupId, topic, partition);
        return Long.valueOf(new String(actual.getData().forPath(offsetPath)));
    }

}
