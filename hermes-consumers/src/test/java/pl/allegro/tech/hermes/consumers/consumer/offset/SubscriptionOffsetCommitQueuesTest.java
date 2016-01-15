package pl.allegro.tech.hermes.consumers.consumer.offset;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.test.MessageBuilder;
import pl.allegro.tech.hermes.consumers.test.Wait;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SubscriptionOffsetCommitQueuesTest {

    private static final int FIRST_PARTITION = 0;
    private static final int SECOND_PARTITION = 1;
    private static final KafkaTopicName KAFKA_TOPIC = KafkaTopicName.valueOf("kafka_topic");

    private Subscription subscription = Subscription.Builder.subscription()
            .applyDefaults().withSubscriptionPolicy(
                    SubscriptionPolicy.Builder.subscriptionPolicy()
                            .applyDefaults().withRate(1000).withMessageTtl(5).build()
            ).build();

    private HermesMetrics hermesMetrics = Mockito.mock(HermesMetrics.class);

    private ConfigFactory configFactory = Mockito.mock(ConfigFactory.class);

    private SubscriptionOffsetCommitQueues subscriptionOffsetCommitQueues =
            new SubscriptionOffsetCommitQueues(subscription, hermesMetrics, Clock.systemDefaultZone(), configFactory);

    @Before
    public void setUp() {

        Mockito.when(configFactory.getIntProperty(Configs.CONSUMER_OFFSET_COMMIT_QUEUE_ALERT_SIZE)).thenReturn(100);
        Mockito.when(configFactory.getIntProperty(Configs.CONSUMER_OFFSET_COMMIT_QUEUE_ALERT_MINIMAL_IDLE_PERIOD)).thenReturn(10);

        subscriptionOffsetCommitQueues = new SubscriptionOffsetCommitQueues(subscription, hermesMetrics, Clock.systemDefaultZone(), configFactory);
    }

    @Test
    public void shouldReturnNullForEmptyHelper() {
        //when
        List<PartitionOffset> offsets = subscriptionOffsetCommitQueues.getOffsetsToCommit();

        //then
        assertThat(offsets).isEmpty();
    }

    @Test
    public void shouldReturnZeroWhenOnlyOffsetZeroIsFinished() {
        //given
        partition(FIRST_PARTITION).addOffsets(0).finishOffsets(0);

        //when
        List<PartitionOffset> offsets = subscriptionOffsetCommitQueues.getOffsetsToCommit();

        // then
        assertThat(offsets).containsOnlyOnce(new PartitionOffset(KAFKA_TOPIC, 0, FIRST_PARTITION));
    }

    @Test
    public void shouldReturnNullIfNoMessagesFinished() {
        //given
        partition(FIRST_PARTITION).addOffsets(1, 2, 3);

        //when
        List<PartitionOffset> offsets = subscriptionOffsetCommitQueues.getOffsetsToCommit();

        //then
        assertThat(offsets).isEmpty();
    }

    @Test
    public void shouldReturnLastFinishedOffset() {
        // given
        partition(FIRST_PARTITION).addOffsets(1).finishOffsets(1);

        // when
        List<PartitionOffset> offsets = subscriptionOffsetCommitQueues.getOffsetsToCommit();

        // then
        assertThat(offsets).containsOnly(new PartitionOffset(KAFKA_TOPIC, 1, FIRST_PARTITION));
    }

    @Test
    public void shouldReturnLastFinishedOffsetForCorrectPartition() {
        // given
        partition(FIRST_PARTITION).addOffsets(1).finishOffsets(1);
        partition(SECOND_PARTITION).addOffsets(2).finishOffsets(2);

        // when
        List<PartitionOffset> offsets = subscriptionOffsetCommitQueues.getOffsetsToCommit();

        // then
        assertThat(offsets).containsOnly(new PartitionOffset(KAFKA_TOPIC, 1, FIRST_PARTITION), new PartitionOffset(KAFKA_TOPIC, 2, SECOND_PARTITION));
    }

    @Test
    public void shouldNotCommitUnchangedOffset() {
        //given
        partition(FIRST_PARTITION).addOffsets(1).finishOffsets(1);

        //when
        subscriptionOffsetCommitQueues.getOffsetsToCommit();
        Wait.forCacheInvalidation();
        List<PartitionOffset> offsets = subscriptionOffsetCommitQueues.getOffsetsToCommit();

        //then
        assertThat(offsets).isEmpty();
    }

    @Test
    public void shouldCommitWhenOffsetFinallyChanges() {
        //given
        Partition partition = partition(FIRST_PARTITION);
        partition.addOffsets(1).finishOffsets(1);

        //when
        subscriptionOffsetCommitQueues.getOffsetsToCommit();
        Wait.forCacheInvalidation();
        partition.addOffsets(2).finishOffsets(2);


        //then
        List<PartitionOffset> offsets = subscriptionOffsetCommitQueues.getOffsetsToCommit();
        assertThat(offsets).containsOnly(new PartitionOffset(KAFKA_TOPIC, 2, FIRST_PARTITION));
    }

    private Partition partition(final int partition) {
        return new Partition(partition);
    }

    private class Partition {
        private final int partition;

        public Partition(int partition) {

            this.partition = partition;
        }

        public Partition addOffsets(long... offsets) {
            for (long offset : offsets) {
                subscriptionOffsetCommitQueues.put(messageWithPartitionOffset(offset));
            }
            return this;
        }

        private Partition finishOffsets(long... offsets) {
            for (long offset : offsets) {
                subscriptionOffsetCommitQueues.remove(messageWithPartitionOffset(offset));
            }
            return this;
        }

        private Message messageWithPartitionOffset(long offset) {
            return MessageBuilder.withTestMessage().withContent(new byte[partition])
                    .withPartitionOffset(new PartitionOffset(KAFKA_TOPIC, offset, partition))
                    .build();
        }
    }
}
