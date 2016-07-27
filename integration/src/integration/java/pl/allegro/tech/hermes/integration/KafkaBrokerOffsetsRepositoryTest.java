package pl.allegro.tech.hermes.integration;

import com.codahale.metrics.MetricRegistry;
import com.google.common.net.HostAndPort;
import kafka.network.BlockingChannel;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.consumers.consumer.offset.FailedToCommitOffsets;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsToCommit;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartition;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.BlockingChannelFactory;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.BrokerOffsetsRepository;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class KafkaBrokerOffsetsRepositoryTest extends IntegrationTest {

    private final NamespaceKafkaNamesMapper mapper = new NamespaceKafkaNamesMapper(KAFKA_NAMESPACE);

    private final HostnameResolver hostnameResolver = () -> "localhost";

    private final String kafkaHost = "localhost";

    private final HermesMetrics metrics = new HermesMetrics(new MetricRegistry(), new PathsCompiler("localhost"));

    private int kafkaPort;

    private KafkaTopicName kafkaTopicName;
    private SubscriptionName subscriptionName;

    private final int readTimeout = 60_000;
    private final int channelExpTime = 60_000;

    private BrokerOffsetsRepository offsetStorage;

    private BlockingChannelFactory blockingChannelFactory;

    @BeforeClass
    public void setUpClass() throws Exception {
        kafkaPort = SharedServices.services().kafkaStarter().instance().serverConfig().port();

        Topic topic = topic("brokerMessageCommiter", "topic").build();
        kafkaTopicName = mapper.toKafkaTopics(topic).getPrimary().name();
        Subscription subscription = subscription(topic, "subscription").build();
        subscriptionName = subscription.getQualifiedName();

        operations.buildTopic(topic);
        operations.createSubscription(topic, subscriptionName.getName(), HTTP_ENDPOINT_URL);

        wait.waitUntilConsumerMetadataAvailable(subscription, kafkaHost, kafkaPort);

        blockingChannelFactory = new BlockingChannelFactory(HostAndPort.fromParts(kafkaHost, kafkaPort), readTimeout);
        offsetStorage = new BrokerOffsetsRepository(
                blockingChannelFactory, channelExpTime, mapper, Clock.systemDefaultZone(), hostnameResolver, metrics
        );
    }

    @Test
    public void shouldCommitAllOffsetsExactlyAsTheyWerePassed() throws Exception {
        //given
        OffsetsToCommit offsetsToCommit = new OffsetsToCommit();
        offsetsToCommit.add(offset(0, 10));
        offsetsToCommit.add(offset(1, 8));

        //when
        offsetStorage.commit(offsetsToCommit);

        //then
        assertThat(offsetStorage.findOffset(partition(0))).isEqualTo(10);
        assertThat(offsetStorage.findOffset(partition(1))).isEqualTo(8);
    }

    @Test
    public void shouldNotAllowOnMovingOffsetToThePast() throws Exception {
        //given
        offsetStorage.moveOffset(offset(0, 10));

        //when
        offsetStorage.moveOffset(offset(0, 15));

        //then
        assertThat(offsetStorage.findOffset(partition(0))).isEqualTo(10);
    }

    @Test
    public void shouldReloadInterruptedBlockingChannelOnRetryAfterFailure() throws Exception {
        // given
        OffsetsToCommit offsetsToCommit = new OffsetsToCommit();
        offsetsToCommit.add(offset(0, 20));


        BlockingChannelFactory blockingChannelFactory = new UnreliableBlockingChannelFactory(
                HostAndPort.fromParts(kafkaHost, kafkaPort), readTimeout
        );
        BrokerOffsetsRepository offsetStorage = new BrokerOffsetsRepository(
                blockingChannelFactory, channelExpTime, mapper, Clock.systemDefaultZone(), hostnameResolver, metrics
        );

        // when
        FailedToCommitOffsets failedToCommitOffsets = offsetStorage.commit(offsetsToCommit);

        // and
        offsetStorage.commit(offsetsToCommit);

        // then
        assertThat(failedToCommitOffsets.failedOffsets()).containsExactly(offset(0, 20));
        assertThat(offsetStorage.findOffset(partition(0))).isEqualTo(20);
    }

    private SubscriptionPartitionOffset offset(int partition, long offset) {
        return SubscriptionPartitionOffset.subscriptionPartitionOffset(
                kafkaTopicName.asString(), subscriptionName.getQualifiedName(), partition, offset
        );
    }

    private SubscriptionPartition partition(int partition) {
        return SubscriptionPartition.subscriptionPartition(
                kafkaTopicName.asString(), subscriptionName.getQualifiedName(), partition
        );
    }

    private static final class UnreliableBlockingChannelFactory extends BlockingChannelFactory {

        private final AtomicInteger requestCount = new AtomicInteger(0);

        UnreliableBlockingChannelFactory(HostAndPort broker, int readTimeout) {
            super(broker, readTimeout);
        }

        @Override
        public BlockingChannel create(ConsumerGroupId consumerGroupId) {
            return requestCount.getAndIncrement() == 0 ? createDeadChannel() : super.create(consumerGroupId);
        }

        private BlockingChannel createDeadChannel() {
            return new BlockingChannel(
                    "localhost", 12345,
                    BlockingChannel.UseDefaultBufferSize(), BlockingChannel.UseDefaultBufferSize(),
                    10
            );
        }
    }
}