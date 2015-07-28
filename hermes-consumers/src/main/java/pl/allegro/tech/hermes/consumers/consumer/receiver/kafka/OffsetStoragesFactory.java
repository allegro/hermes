package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.google.common.collect.Lists;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.broker.BlockingChannelFactory;
import pl.allegro.tech.hermes.common.broker.KafkaOffsetsStorage;
import pl.allegro.tech.hermes.common.broker.OffsetsStorage;
import pl.allegro.tech.hermes.common.broker.ZookeeperOffsetsStorage;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.time.Clock;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;

public class OffsetStoragesFactory implements Factory<List<OffsetsStorage>> {

    private final boolean dualCommitEnabled;
    private final OffsetsStorageType offsetStorageType;
    private final CuratorFramework curator;
    private final BlockingChannelFactory blockingChannelFactory;
    private final Clock clock;

    @Inject
    public OffsetStoragesFactory(@Named(CuratorType.KAFKA) CuratorFramework curator,
                                 BlockingChannelFactory blockingChannelFactory,
                                 Clock clock,
                                 ConfigFactory configFactory) {
        this.curator = curator;
        this.blockingChannelFactory = blockingChannelFactory;
        this.clock = clock;
        this.dualCommitEnabled = configFactory.getBooleanProperty(Configs.KAFKA_CONSUMER_DUAL_COMMIT_ENABLED);
        this.offsetStorageType = OffsetsStorageType.valueOf(configFactory.getStringProperty(Configs.KAFKA_CONSUMER_OFFSETS_STORAGE).toUpperCase());
    }

    @Override
    public List<OffsetsStorage> provide() {
        List<OffsetsStorage> offsetsStorages = Lists.newArrayList();
        if (dualCommitEnabled || OffsetsStorageType.ZOOKEEPER == offsetStorageType) {
            offsetsStorages.add(new ZookeeperOffsetsStorage(curator));
        }
        if (dualCommitEnabled || OffsetsStorageType.KAFKA == offsetStorageType) {
            offsetsStorages.add(new KafkaOffsetsStorage(blockingChannelFactory, clock));
        }
        return Collections.unmodifiableList(offsetsStorages);
    }

    @Override
    public void dispose(List<OffsetsStorage> instance) {

    }
}
