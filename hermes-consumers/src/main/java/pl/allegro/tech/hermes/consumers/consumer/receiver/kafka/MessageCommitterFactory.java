package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.google.common.net.HostAndPort;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker.BlockingChannelFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker.BrokerMessageCommitter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.zookeeper.ZookeeperMessageCommitter;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessageCommitterFactory implements Factory<List<MessageCommitter>> {

    private final OffsetsStorage offsetsStorage;
    private final Clock clock;
    private final CuratorFramework curatorFramework;
    private final HostnameResolver hostnameResolver;
    private final ConfigFactory configFactory;
    private final boolean dualCommitEnabled;

    @Inject
    public MessageCommitterFactory(ConfigFactory configFactory,
                                   Clock clock,
                                   @Named(CuratorType.KAFKA) CuratorFramework curatorFramework,
                                   HostnameResolver hostnameResolver) {
        this.configFactory = configFactory;
        this.clock = clock;
        this.curatorFramework = curatorFramework;
        this.hostnameResolver = hostnameResolver;
        this.offsetsStorage = OffsetsStorage.valueOf(configFactory.getStringProperty(Configs.KAFKA_CONSUMER_OFFSETS_STORAGE).toUpperCase());
        this.dualCommitEnabled = configFactory.getBooleanProperty(Configs.KAFKA_CONSUMER_DUAL_COMMIT_ENABLED);
    }

    @Override
    public List<MessageCommitter> provide() {
        List<MessageCommitter> committers = new ArrayList<>();
        if (dualCommitEnabled || OffsetsStorage.KAFKA == offsetsStorage) {
            committers.add(brokerMessageCommitter(configFactory, clock));
        }
        if (dualCommitEnabled || OffsetsStorage.ZOOKEEPER == offsetsStorage) {
            committers.add(new ZookeeperMessageCommitter(curatorFramework));
        }
        return committers;
    }

    private MessageCommitter brokerMessageCommitter(ConfigFactory configFactory, Clock clock) {
        return new BrokerMessageCommitter(blockingChannelFactory(configFactory), clock, hostnameResolver,
                configFactory.getIntProperty(Configs.KAFKA_CONSUMER_OFFSET_COMMITTER_BROKER_CONNECTION_EXPIRATION));
    }

    private BlockingChannelFactory blockingChannelFactory(ConfigFactory configFactory) {
        HostAndPort broker = HostAndPort.fromString(findAnyBroker(configFactory.getStringProperty(Configs.KAFKA_BROKER_LIST)));

        return new BlockingChannelFactory(broker, configFactory.getIntProperty(Configs.KAFKA_CONSUMER_METADATA_READ_TIMEOUT));
    }

    private String findAnyBroker(String brokerList) {
        return Arrays.stream(brokerList.split(",")).findAny().get();
    }

    @Override
    public void dispose(List<MessageCommitter> instance) {
    }
}
