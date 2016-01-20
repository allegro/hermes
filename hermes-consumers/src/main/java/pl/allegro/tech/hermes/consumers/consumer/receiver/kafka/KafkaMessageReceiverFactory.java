package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.util.Properties;

public class KafkaMessageReceiverFactory implements ReceiverFactory {

    private final ConfigFactory configFactory;
    private final MessageContentWrapper messageContentWrapper;
    private final HermesMetrics hermesMetrics;
    private final Clock clock;
    private final KafkaNamesMapper kafkaNamesMapper;

    @Inject
    public KafkaMessageReceiverFactory(ConfigFactory configFactory, MessageContentWrapper messageContentWrapper,
                                       HermesMetrics hermesMetrics, Clock clock, KafkaNamesMapper kafkaNamesMapper) {
        this.configFactory = configFactory;
        this.messageContentWrapper = messageContentWrapper;
        this.hermesMetrics = hermesMetrics;
        this.clock = clock;
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    @Override
    public MessageReceiver createMessageReceiver(Topic receivingTopic, Subscription subscription) {
        return create(receivingTopic, createConsumerConfig(kafkaNamesMapper.toConsumerGroupId(subscription)), subscription);
    }

    MessageReceiver create(Topic receivingTopic, ConsumerConfig consumerConfig, Subscription subscription) {
        return new KafkaMessageReceiver(
                receivingTopic,
                Consumer.createJavaConsumerConnector(consumerConfig),
                messageContentWrapper,
                hermesMetrics.timer(Timers.READ_LATENCY),
                clock,
                kafkaNamesMapper,
                configFactory.getIntProperty(Configs.KAFKA_STREAM_COUNT),
                configFactory.getIntProperty(Configs.KAFKA_CONSUMER_TIMEOUT_MS),
                subscription.toSubscriptionName());
    }

    private ConsumerConfig createConsumerConfig(ConsumerGroupId groupId) {
        Properties props = new Properties();

        props.put("group.id", groupId.asString());
        props.put("zookeeper.connect", configFactory.getStringProperty(Configs.KAFKA_ZOOKEEPER_CONNECT_STRING));
        props.put("zookeeper.connection.timeout.ms", configFactory.getIntPropertyAsString(Configs.ZOOKEEPER_CONNECTION_TIMEOUT));
        props.put("zookeeper.session.timeout.ms", configFactory.getIntPropertyAsString(Configs.ZOOKEEPER_SESSION_TIMEOUT));
        props.put("zookeeper.sync.time.ms", configFactory.getIntPropertyAsString(Configs.ZOOKEEPER_SYNC_TIME));
        props.put("auto.commit.enable", "false");
        props.put("fetch.wait.max.ms", "10000");
        props.put("consumer.timeout.ms", configFactory.getIntPropertyAsString(Configs.KAFKA_CONSUMER_TIMEOUT_MS));
        props.put("auto.offset.reset", configFactory.getStringProperty(Configs.KAFKA_CONSUMER_AUTO_OFFSET_RESET));
        props.put("offsets.storage", configFactory.getStringProperty(Configs.KAFKA_CONSUMER_OFFSETS_STORAGE));
        props.put("dual.commit.enabled", Boolean.toString(configFactory.getBooleanProperty(Configs.KAFKA_CONSUMER_DUAL_COMMIT_ENABLED)));

        return new ConsumerConfig(props);
    }

}
