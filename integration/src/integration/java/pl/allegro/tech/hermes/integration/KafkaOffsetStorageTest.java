package pl.allegro.tech.hermes.integration;

import com.google.common.net.HostAndPort;
import com.jayway.awaitility.Duration;
import kafka.api.ConsumerMetadataRequest;
import kafka.common.ErrorMapping;
import kafka.javaapi.ConsumerMetadataResponse;
import kafka.network.BlockingChannel;
import kafka.server.KafkaConfig;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.broker.BlockingChannelFactory;
import pl.allegro.tech.hermes.common.broker.KafkaOffsetsStorage;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.integration.env.SharedServices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KafkaOffsetStorageTest extends IntegrationTest {

    private final String kafkaHost = "localhost";
    private final TopicName topicName = new TopicName("offsetStorageGroup", "topic");
    private final String subscriptionName = "subscription";
    private Subscription subscription;

    private int readTimeout = 60_000;

    private int kafkaPort;

    private KafkaOffsetsStorage offsetsStorage;

    @BeforeMethod
    public void setUp() throws Exception {
        subscription = new Subscription.Builder().withTopicName(topicName).withName(subscriptionName).build();

        HostnameResolver hostnameResolver = mock(HostnameResolver.class);
        when(hostnameResolver.resolve()).thenReturn(kafkaHost);

        KafkaConfig kafkaConfig = SharedServices.services().kafkaStarter().instance().serverConfig();
        kafkaPort = kafkaConfig.port();

        operations.buildSubscription(topicName.getGroupName(), topicName.getName(), subscriptionName, HTTP_ENDPOINT_URL);

        wait.waitUntilConsumerMetadataAvailable(subscription, "localhost", kafkaPort);

        BlockingChannelFactory blockingChannelFactory = new BlockingChannelFactory(HostAndPort.fromParts(kafkaHost, kafkaPort), readTimeout);
        offsetsStorage = new KafkaOffsetsStorage(blockingChannelFactory, mock(Clock.class));
    }

    @Test
    public void shouldCommitOffset() throws Exception {
        //given
        int partition = 0;
        int offset = 10;

        //when
        offsetsStorage.setSubscriptionOffset(topicName, subscriptionName, partition, offset);

        //then
        assertThat(offsetsStorage.getSubscriptionOffset(topicName, subscriptionName, partition)).isEqualTo(offset);
    }
}
