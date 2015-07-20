package pl.allegro.tech.hermes.integration;

import com.google.common.net.HostAndPort;
import com.jayway.awaitility.Duration;
import jersey.repackaged.com.google.common.collect.Lists;
import kafka.api.ConsumerMetadataRequest;
import kafka.common.ErrorMapping;
import kafka.common.OffsetMetadataAndError;
import kafka.common.TopicAndPartition;
import kafka.javaapi.ConsumerMetadataResponse;
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
import pl.allegro.tech.hermes.common.broker.BlockingChannelFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker.BrokerMessageCommitter;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;
import pl.allegro.tech.hermes.integration.env.SharedServices;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BrokerMessageCommitterTest extends IntegrationTest {

    private final String kafkaHost = "localhost";
    private final String groupName = "brokerMessageCommiter";
    private final String topicName = "topic";
    private final String subscriptionName = "subscription";
    private Subscription subscription;

    private int readTimeout = 60;
    private int channelExpTime = 60_000;

    private MessageCommitter messageCommiter;
    private int kafkaPort;

    @BeforeMethod
    public void setUp() throws Exception {
        subscription = new Subscription.Builder().withTopicName(groupName, topicName).withName(subscriptionName).build();

        HostnameResolver hostnameResolver = mock(HostnameResolver.class);
        when(hostnameResolver.resolve()).thenReturn(kafkaHost);

        KafkaConfig kafkaConfig = SharedServices.services().kafkaStarter().instance().serverConfig();
        kafkaPort = kafkaConfig.port();

        operations.buildSubscription(groupName, topicName, subscriptionName, HTTP_ENDPOINT_URL);

        waitUntilConsumerMetadataAvailable();

        BlockingChannelFactory blockingChannelFactory = new BlockingChannelFactory(HostAndPort.fromParts(kafkaHost, kafkaPort), readTimeout);
        messageCommiter = new BrokerMessageCommitter(blockingChannelFactory, new SystemClock(), hostnameResolver, channelExpTime);
    }

    private void waitUntilConsumerMetadataAvailable() {
        BlockingChannel channel = createBlockingChannel();
        channel.connect();

        wait.until(() -> {
            channel.send(new ConsumerMetadataRequest(subscription.getId(), ConsumerMetadataRequest.CurrentVersion(), 0, "0"));
            ConsumerMetadataResponse metadataResponse = ConsumerMetadataResponse.readFrom(channel.receive().buffer());
            return metadataResponse.errorCode() == ErrorMapping.NoError();
        }, Duration.ONE_MINUTE);

        channel.disconnect();
    }

    private BlockingChannel createBlockingChannel() {
        return new BlockingChannel(kafkaHost, kafkaPort,
                BlockingChannel.UseDefaultBufferSize(),
                BlockingChannel.UseDefaultBufferSize(),
                readTimeout);
    }

    @Test
    public void shouldCommitOffset() throws Exception {
        //given
        int partition = 0;
        int offset = 10;
        PartitionOffset partitionOffset = new PartitionOffset(offset, partition);

        //when
        messageCommiter.commitOffset(subscription, partitionOffset);

        //then
        assertThat(readOffset(subscription, partition)).isEqualTo(offset);
    }

    private long readOffset(Subscription subscription, int partition) {
        BlockingChannel channel = createBlockingChannel();
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