package pl.allegro.tech.hermes.integration;

import com.google.common.net.HostAndPort;
import kafka.network.BlockingChannel;
import kafka.server.KafkaConfig;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.time.SystemClock;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.BlockingChannelFactory;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.BrokerOffsetsRepository;
import pl.allegro.tech.hermes.integration.env.SharedServices;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

public class KafkaBrokerOffsetsRepositoryTest extends IntegrationTest {

    private final String kafkaHost = "localhost";
    private final Topic topic = topic().applyDefaults().withName("brokerMessageCommiter", "topic").build();
    private final String subscriptionName = "subscription";
    private KafkaTopicName kafkaTopicName;
    private Subscription subscription;
    private HostnameResolver hostnameResolver;

    private int readTimeout = 60_000;
    private int channelExpTime = 60_000;

    private BrokerOffsetsRepository offsetStorage;
    private BlockingChannelFactory blockingChannelFactory;
    private int kafkaPort;

    @BeforeMethod
    public void setUp() throws Exception {
        kafkaTopicName = new NamespaceKafkaNamesMapper(KAFKA_NAMESPACE).toKafkaTopics(topic).getPrimary().name();
        subscription = new Subscription.Builder().withTopicName(topic.getName()).withName(subscriptionName).build();

        hostnameResolver = mock(HostnameResolver.class);
        when(hostnameResolver.resolve()).thenReturn(kafkaHost);

        KafkaConfig kafkaConfig = SharedServices.services().kafkaStarter().instance().serverConfig();
        kafkaPort = kafkaConfig.port();

        operations.buildTopic(topic);
        operations.createSubscription(topic, subscriptionName, HTTP_ENDPOINT_URL);

        wait.waitUntilConsumerMetadataAvailable(subscription, kafkaHost, kafkaPort);

        blockingChannelFactory = new BlockingChannelFactory(HostAndPort.fromParts(kafkaHost, kafkaPort), readTimeout);
        offsetStorage = new BrokerOffsetsRepository(blockingChannelFactory, new SystemClock(), hostnameResolver, new NamespaceKafkaNamesMapper(KAFKA_NAMESPACE), channelExpTime);
    }

    @Test
    public void shouldCommitOffset() throws Exception {
        //given
        PartitionOffset partitionOffset = new PartitionOffset(kafkaTopicName, 10, 0);

        //when
        offsetStorage.save(subscription, partitionOffset);

        //then
        assertThat(offsetStorage.find(subscription, kafkaTopicName, partitionOffset.getPartition())).isEqualTo(partitionOffset.getOffset());
    }

    @Test
    public void shouldNotCommitOffsetInTheFutureWhenSavingOffsetInThePast() throws Exception {
        //given
        PartitionOffset oldOffset = new PartitionOffset(kafkaTopicName, 10, 0);
        offsetStorage.save(subscription, oldOffset);

        //when
        offsetStorage.saveIfOffsetInThePast(subscription, new PartitionOffset(kafkaTopicName, 15, oldOffset.getPartition()));

        //then
        assertThat(offsetStorage.find(subscription, kafkaTopicName, oldOffset.getPartition())).isEqualTo(oldOffset.getOffset());
    }

    @Test
    public void shouldSetOffsetEvenIfPartitionWasNotCommittedPreviously() throws Exception {
        //when
        offsetStorage.save(subscription, new PartitionOffset(kafkaTopicName, -1, 0));

        //when
        offsetStorage.saveIfOffsetInThePast(subscription, new PartitionOffset(kafkaTopicName, 0, 0));

        //then
        assertThat(offsetStorage.find(subscription, kafkaTopicName, 0)).isEqualTo(0);
    }

    @Test
    public void shouldReloadInterruptedBlockingChannelOnRetryAfterFailure() throws Exception {
        // given
        PartitionOffset partitionOffset = new PartitionOffset(kafkaTopicName, 20, 0);
        blockingChannelFactory = new UnreliableBlockingChannelFactory(HostAndPort.fromParts(kafkaHost, kafkaPort), readTimeout);
        offsetStorage = new BrokerOffsetsRepository(blockingChannelFactory, new SystemClock(), hostnameResolver, new NamespaceKafkaNamesMapper(KAFKA_NAMESPACE), channelExpTime);

        // when
        try {
            offsetStorage.save(subscription, partitionOffset);
        } catch (Exception e) {}

        // and
        offsetStorage.save(subscription, partitionOffset);

        // then
        assertThat(offsetStorage.find(subscription, kafkaTopicName, partitionOffset.getPartition())).isEqualTo(partitionOffset.getOffset());
    }

    private static class UnreliableBlockingChannelFactory extends BlockingChannelFactory {

        private final AtomicInteger requestCount = new AtomicInteger(0);

        public UnreliableBlockingChannelFactory(HostAndPort broker, int readTimeout) {
            super(broker, readTimeout);
        }

        @Override
        public BlockingChannel create(ConsumerGroupId consumerGroupId) {
            return requestCount.getAndIncrement() == 0 ?
                    new BlockingChannel("localhost", 12345, BlockingChannel.UseDefaultBufferSize(), BlockingChannel.UseDefaultBufferSize(), 10)
                    : super.create(consumerGroupId);
        }
    }
}