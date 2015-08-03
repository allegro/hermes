package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.google.common.collect.Lists;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsStorage;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;

public class OffsetStoragesFactory implements Factory<List<OffsetsStorage>> {

    private final OffsetsStorage kafkaOffsetsStorage;
    private final OffsetsStorage zookeeperOffsetsStorage;

    private final boolean dualCommitEnabled;
    private final OffsetsStorageType offsetStorageType;

    @Inject
    public OffsetStoragesFactory(@Named("kafkaOffsetsStorage") OffsetsStorage kafkaOffsetsStorage,
                                 @Named("zookeeperOffsetsStorage") OffsetsStorage zookeeperOffsetsStorage,
                                 ConfigFactory configFactory) {
        this.zookeeperOffsetsStorage = zookeeperOffsetsStorage;
        this.kafkaOffsetsStorage = kafkaOffsetsStorage;

        this.dualCommitEnabled = configFactory.getBooleanProperty(Configs.KAFKA_CONSUMER_DUAL_COMMIT_ENABLED);
        this.offsetStorageType = OffsetsStorageType.valueOf(configFactory.getStringProperty(Configs.KAFKA_CONSUMER_OFFSETS_STORAGE).toUpperCase());
    }

    @Override
    public List<OffsetsStorage> provide() {
        List<OffsetsStorage> offsetsStorages = Lists.newArrayList();
        if (dualCommitEnabled || OffsetsStorageType.ZOOKEEPER == offsetStorageType) {
            offsetsStorages.add(zookeeperOffsetsStorage);
        }
        if (dualCommitEnabled || OffsetsStorageType.KAFKA == offsetStorageType) {
            offsetsStorages.add(kafkaOffsetsStorage);
        }
        return Collections.unmodifiableList(offsetsStorages);
    }

    @Override
    public void dispose(List<OffsetsStorage> instance) {

    }
}
