package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaZookeeperPaths;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsToCommit;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.zookeeper.ZookeeperMessageCommitter;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

public class ZookeeperMessageCommitterTest extends ZookeeperBaseTest {

    private static final KafkaTopicName KAFKA_TOPIC = KafkaTopicName.valueOf("kafka_topic");

    private final NamespaceKafkaNamesMapper mapper = new NamespaceKafkaNamesMapper("zkMessageCommitter");

    private final HermesMetrics metrics = new HermesMetrics(new MetricRegistry(), new PathsCompiler("localhost"));

    private ZookeeperMessageCommitter zookeeperMessageCommitter = new ZookeeperMessageCommitter(
            zookeeperClient, mapper, metrics
    );

    @Before
    public void initialize() throws Exception {
        super.deleteData("/consumers");
    }

    @Test
    public void shouldCommitOffsetsIfNoEntryExists() throws Exception {
        // given
        OffsetsToCommit offsetsToCommit = new OffsetsToCommit();
        offsetsToCommit.add(offset(0, 15));

        //when
        zookeeperMessageCommitter.commitOffsets(offsetsToCommit);

        //then
        offsetsSaved(offset(0, 15));
    }

    @Test
    public void shouldOverwriteCommitEntry() throws Exception {
        //given
        OffsetsToCommit offsetsToCommit = new OffsetsToCommit();
        offsetsToCommit.add(offset(0, 15));

        zookeeperMessageCommitter.commitOffsets(offsetsToCommit);

        OffsetsToCommit newOffsetsToCommit = new OffsetsToCommit();
        newOffsetsToCommit.add(offset(0, 17));

        //when
        zookeeperMessageCommitter.commitOffsets(newOffsetsToCommit);

        //then
        offsetsSaved(offset(0, 17));
    }

    @Test
    public void shouldCommitOffsetToPartitionPath() throws Exception {
        //given
        OffsetsToCommit offsetsToCommit = new OffsetsToCommit();
        offsetsToCommit.add(offset(0, 15));
        offsetsToCommit.add(offset(1, 10));

        // when
        zookeeperMessageCommitter.commitOffsets(offsetsToCommit);

        //then
        offsetsSaved(offset(0, 15), offset(1, 10));
    }

    @Test
    public void shouldRemoveCommittedOffsetWhenRemoveCalled() throws Exception {
        //given
        OffsetsToCommit offsetsToCommit = new OffsetsToCommit();
        offsetsToCommit.add(offset(2, 1));

        zookeeperMessageCommitter.commitOffsets(offsetsToCommit);

        //when
        zookeeperMessageCommitter.removeOffset(new TopicName("group", "topic"), "subscription", KAFKA_TOPIC, 2);

        //then
        String path = KafkaZookeeperPaths.offsetsPath(
                mapper.toConsumerGroupId(SubscriptionName.fromString("group.topic$subscription")),
                KAFKA_TOPIC
        ) + "/" + 2;
        assertThat(zookeeperClient.checkExists().forPath(path)).isNull();
    }

    private SubscriptionPartitionOffset offset(int partition, long offset) {
        return SubscriptionPartitionOffset.subscriptionPartitionOffset(
                KAFKA_TOPIC.asString(), "group.topic$subscription", partition, offset
        );
    }

    private void offsetsSaved(SubscriptionPartitionOffset... offsets) {
        try {
            for (SubscriptionPartitionOffset offset : offsets) {
                long savedOffset = getOffsetForPath(
                        KafkaZookeeperPaths.offsetsPath(
                                mapper.toConsumerGroupId(offset.getSubscriptionName()),
                                KAFKA_TOPIC
                        ) + "/" + offset.getPartition()
                );
                assertThat(savedOffset).isEqualTo(offset.getOffset());
            }
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private long getOffsetForPath(String path) throws Exception {
        byte[] bytes = zookeeperClient.getData().forPath(path);
        return Long.parseLong(new String(bytes, Charset.defaultCharset()));
    }

}
