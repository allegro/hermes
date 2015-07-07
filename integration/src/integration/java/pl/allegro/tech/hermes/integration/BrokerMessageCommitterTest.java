package pl.allegro.tech.hermes.integration;

import com.google.common.net.HostAndPort;
import jersey.repackaged.com.google.common.collect.Lists;
import kafka.common.OffsetMetadataAndError;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetFetchRequest;
import kafka.javaapi.OffsetFetchResponse;
import kafka.network.BlockingChannel;
import kafka.server.KafkaConfig;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.time.SystemClock;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker.BlockingChannelFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker.BrokerMessageCommitter;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;
import pl.allegro.tech.hermes.integration.env.SharedServices;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BrokerMessageCommitterTest extends IntegrationTest {

    String groupName = "brokerMessageCommiter";
    String topicName = "topic";
    String subscriptionName = "subscription";

    private MessageCommitter messageCommiter;
    private int kafkaPort;

    private BlockingChannelFactory blockingChannelFactory;

    @BeforeMethod
    public void setUp() throws Exception {
        HostnameResolver hostnameResolver = mock(HostnameResolver.class);
        when(hostnameResolver.resolve()).thenReturn("localhost");

        operations.buildSubscription(groupName, topicName, subscriptionName, HTTP_ENDPOINT_URL);
        wait.untilOffsetsTopicCreated();

        KafkaConfig kafkaConfig = SharedServices.services().kafkaStarter().instance().serverConfig();

        int readTimeout = 60;
        int channelExpTime = 60_000;

        kafkaPort = kafkaConfig.port();
        blockingChannelFactory = new BlockingChannelFactory(HostAndPort.fromParts("localhost", kafkaPort), readTimeout);
        messageCommiter = new BrokerMessageCommitter(blockingChannelFactory,
                new SystemClock(),
                hostnameResolver,
                channelExpTime);
    }

    @Test
    public void shouldCommitOffset() throws Exception {
        //given
        Subscription subscription = new Subscription.Builder().withTopicName(groupName, topicName).withName(subscriptionName).build();
        int partition = 0;
        int offset = 10;
        PartitionOffset partitionOffset = new PartitionOffset(offset, partition);

        //when
        messageCommiter.commitOffset(subscription, partitionOffset);

        //then
        assertThat(readOffset(subscription, partition)).isEqualTo(offset);
    }

    private long readOffset(Subscription subscription, int partition) {
        BlockingChannel channel = blockingChannelFactory.create(subscription.getId());
        channel.connect();

        TopicAndPartition topicAndPartition = new TopicAndPartition(subscription.getTopicName().qualifiedName(), partition);
        List<TopicAndPartition> partitions = Lists.newArrayList(topicAndPartition);

        OffsetFetchRequest fetchRequest = new OffsetFetchRequest(
                subscription.getId(),
                partitions,
                (short) 1,
                0,
                "clientId");

        channel.send(fetchRequest.underlying());
        OffsetFetchResponse fetchResponse = OffsetFetchResponse.readFrom(channel.receive().buffer());
        Map<TopicAndPartition, OffsetMetadataAndError> result = fetchResponse.offsets();
        OffsetMetadataAndError offset = result.get(topicAndPartition);
        channel.disconnect();
        return offset.offset();
    }
}