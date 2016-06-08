package pl.allegro.tech.hermes.integration;

import com.google.common.net.HostAndPort;
import kafka.network.BlockingChannel;
import kafka.server.KafkaConfig;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartition;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.BlockingChannelFactory;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.BrokerOffsetsRepository;
import pl.allegro.tech.hermes.integration.env.SharedServices;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartition.subscriptionPartition;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class KafkaBrokerOffsetsRepositoryTest extends IntegrationTest {

    private final String kafkaHost = "localhost";
    private final Topic topic = topic("brokerMessageCommiter", "topic").build();
    private KafkaTopicName kafkaTopicName;
    private SubscriptionName subscriptionName;
    private HostnameResolver hostnameResolver;

    private int readTimeout = 60_000;
    private int channelExpTime = 60_000;

    private BrokerOffsetsRepository offsetStorage;
    private BlockingChannelFactory blockingChannelFactory;
    private int kafkaPort;

    @BeforeMethod
    public void setUp() throws Exception {
        kafkaTopicName = new NamespaceKafkaNamesMapper(KAFKA_NAMESPACE).toKafkaTopics(topic).getPrimary().name();
        Subscription subscription = subscription(topic, "subscription").build();
        subscriptionName = subscription.toSubscriptionName();

        hostnameResolver = mock(HostnameResolver.class);
        when(hostnameResolver.resolve()).thenReturn(kafkaHost);

        KafkaConfig kafkaConfig = SharedServices.services().kafkaStarter().instance().serverConfig();
        kafkaPort = kafkaConfig.port();

        operations.buildTopic(topic);
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        wait.waitUntilConsumerMetadataAvailable(subscription, kafkaHost, kafkaPort);

        blockingChannelFactory = new BlockingChannelFactory(HostAndPort.fromParts(kafkaHost, kafkaPort), readTimeout);
        offsetStorage = new BrokerOffsetsRepository(blockingChannelFactory, Clock.systemDefaultZone(), hostnameResolver, new NamespaceKafkaNamesMapper(KAFKA_NAMESPACE), channelExpTime);
    }

    @Test
    public void shouldSaveExactOffset() throws Exception {
        //given
        PartitionOffset partitionOffset = new PartitionOffset(kafkaTopicName, 10, 0);

        //when
        offsetStorage.save(subscriptionPartitionOffset(partitionOffset, subscriptionName));

        //then
        assertThat(offsetStorage.find(subscriptionPartition(kafkaTopicName.asString(), subscriptionName.toString(), 0))).isEqualTo(10);
    }

    @Test
    public void shouldIncrementOffsetByOneWhenCommittingOffset() throws Exception {
        //given
        PartitionOffset partitionOffset = new PartitionOffset(kafkaTopicName, 10, 0);

        //when
        offsetStorage.commit(subscriptionPartitionOffset(partitionOffset, subscriptionName));

        //then
        assertThat(offsetStorage.find(subscriptionPartition(kafkaTopicName.asString(), subscriptionName.toString(), 0))).isEqualTo(11);
    }

    @Test
    public void shouldNotSaveOffsetInTheFutureWhenSavingOffsetInThePast() throws Exception {
        //given
        PartitionOffset oldOffset = new PartitionOffset(kafkaTopicName, 10, 0);
        offsetStorage.save(subscriptionPartitionOffset(oldOffset, subscriptionName));

        //when
        offsetStorage.saveIfOffsetInThePast(subscriptionPartitionOffset(kafkaTopicName.asString(), subscriptionName.toString(), 0, 15));

        //then
        assertThat(offsetStorage.find(subscriptionPartition(kafkaTopicName.asString(), subscriptionName.toString(), 0))).isEqualTo(10);
    }

    @Test
    public void shouldSetOffsetEvenIfPartitionWasNotCommittedPreviously() throws Exception {
        //when
        offsetStorage.save(subscriptionPartitionOffset(kafkaTopicName.asString(), subscriptionName.toString(), 0, -1));

        //when
        offsetStorage.saveIfOffsetInThePast(subscriptionPartitionOffset(kafkaTopicName.asString(), subscriptionName.toString(), 0, 0));

        //then
        assertThat(offsetStorage.find(subscriptionPartition(kafkaTopicName.asString(), subscriptionName.toString(), 0))).isEqualTo(0);
    }

    @Test
    public void shouldReloadInterruptedBlockingChannelOnRetryAfterFailure() throws Exception {
        // given
        PartitionOffset partitionOffset = new PartitionOffset(kafkaTopicName, 20, 0);
        blockingChannelFactory = new UnreliableBlockingChannelFactory(HostAndPort.fromParts(kafkaHost, kafkaPort), readTimeout);
        offsetStorage = new BrokerOffsetsRepository(blockingChannelFactory, Clock.systemDefaultZone(), hostnameResolver, new NamespaceKafkaNamesMapper(KAFKA_NAMESPACE), channelExpTime);

        // when
        try {
            offsetStorage.save(subscriptionPartitionOffset(partitionOffset, subscriptionName));
        } catch (Exception e) {}

        // and
        offsetStorage.save(subscriptionPartitionOffset(partitionOffset, subscriptionName));

        // then
        assertThat(offsetStorage.find(subscriptionPartition(kafkaTopicName.asString(), subscriptionName.toString(), 0))).isEqualTo(partitionOffset.getOffset());
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