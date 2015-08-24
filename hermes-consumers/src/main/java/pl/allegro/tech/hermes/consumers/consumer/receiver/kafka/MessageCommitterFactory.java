package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.BrokerOffsetsRepository;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker.BrokerMessageCommitter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.zookeeper.ZookeeperMessageCommitter;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

public class MessageCommitterFactory implements Factory<List<MessageCommitter>> {

    private final OffsetsStorageType offsetsStorageType;
    private final CuratorFramework curatorFramework;
    private final BrokerOffsetsRepository brokerOffsetsRepository;
    private final KafkaNamesMapper kafkaNamesMapper;
    private final boolean dualCommitEnabled;

    @Inject
    public MessageCommitterFactory(ConfigFactory configFactory,
                                   @Named(CuratorType.KAFKA) CuratorFramework curatorFramework,
                                   BrokerOffsetsRepository brokerOffsetsRepository,
                                   KafkaNamesMapper kafkaNamesMapper) {
        this.curatorFramework = curatorFramework;
        this.brokerOffsetsRepository = brokerOffsetsRepository;
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.offsetsStorageType = OffsetsStorageType.valueOf(configFactory.getStringProperty(Configs.KAFKA_CONSUMER_OFFSETS_STORAGE).toUpperCase());
        this.dualCommitEnabled = configFactory.getBooleanProperty(Configs.KAFKA_CONSUMER_DUAL_COMMIT_ENABLED);
    }

    @Override
    public List<MessageCommitter> provide() {
        List<MessageCommitter> committers = new ArrayList<>();
        if (dualCommitEnabled || OffsetsStorageType.KAFKA == offsetsStorageType) {
            committers.add(new BrokerMessageCommitter(brokerOffsetsRepository));
        }
        if (dualCommitEnabled || OffsetsStorageType.ZOOKEEPER == offsetsStorageType) {
            committers.add(new ZookeeperMessageCommitter(curatorFramework, kafkaNamesMapper));
        }
        return committers;
    }

    @Override
    public void dispose(List<MessageCommitter> instance) {
    }
}
