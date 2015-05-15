package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import kafka.consumer.ConsumerConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;

@RunWith(MockitoJUnitRunner.class)
public class KafkaMessageReceiverFactoryTest {

    private static final String DEFAULT_ZK_CONNECT = "localhost:49181";
    private static final String DEFAULT_TIMEOUT = "5000";
    private static final TopicName TOPIC_NAME = new TopicName("group", "topicName");
    private static final String SUBSCRIPTION_NAME = "subscriptionName";
    private static final String DEFAULT_OFFSET_RESET = "largest";

    final SubscriptionPolicy someSubscriptionPolicy = new SubscriptionPolicy(1, 1, false);

    @Mock
    private ConfigFactory configFactory;

    @Mock
    private KafkaMessageReceiver kafkaMessageReceiver;

    @Captor
    private ArgumentCaptor<ConsumerConfig> argConsumerConfig;

    @Captor
    private ArgumentCaptor<TopicName> argTopic;

    @Spy
    @InjectMocks
    private KafkaMessageReceiverFactory messageReceiverFactory;

    @Before
    public void setUp() {
        when(configFactory.getStringProperty(Configs.KAFKA_ZOOKEEPER_CONNECT_STRING)).thenReturn(DEFAULT_ZK_CONNECT);
        when(configFactory.getIntPropertyAsString(Configs.KAFKA_CONSUMER_TIMEOUT_MS)).thenReturn(DEFAULT_TIMEOUT);
        when(configFactory.getIntPropertyAsString(Configs.ZOOKEEPER_CONNECTION_TIMEOUT)).thenReturn(DEFAULT_TIMEOUT);
        when(configFactory.getIntPropertyAsString(Configs.ZOOKEEPER_SESSION_TIMEOUT)).thenReturn(DEFAULT_TIMEOUT);
        when(configFactory.getIntPropertyAsString(Configs.ZOOKEEPER_SYNC_TIME)).thenReturn(DEFAULT_TIMEOUT);
        when(configFactory.getStringProperty(Configs.KAFKA_CONSUMER_AUTO_OFFSET_RESET)).thenReturn(DEFAULT_OFFSET_RESET);
    }

    @Test
    public void shouldCreateMessageReceiver() throws Exception {
        // given
        Subscription subscription = subscription().withTopicName(TOPIC_NAME).withName(SUBSCRIPTION_NAME)
                .withEndpoint(EndpointAddress.of("http://endpoint.pl")).withSubscriptionPolicy(someSubscriptionPolicy).build();

        doReturn(kafkaMessageReceiver).when(messageReceiverFactory).create(argTopic.capture(), argConsumerConfig.capture());

        // when
        MessageReceiver messageReceiver = messageReceiverFactory.createMessageReceiver(subscription);

        // then
        assertThat(messageReceiver).isInstanceOf(KafkaMessageReceiver.class);
        assertThat(argTopic.getValue()).isEqualTo(TOPIC_NAME);
        ConsumerConfig consumerConfig = argConsumerConfig.getValue();
        assertThat(consumerConfig.autoCommitEnable()).isFalse();
        assertThat(consumerConfig.zkConnect()).isEqualTo(DEFAULT_ZK_CONNECT);
        assertThat(consumerConfig.consumerTimeoutMs()).isEqualTo(Integer.valueOf(DEFAULT_TIMEOUT));
    }

}
