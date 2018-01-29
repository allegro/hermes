package pl.allegro.tech.hermes.common.di.factories;

import com.google.common.primitives.Ints;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperProperties;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.dc.DcNameProvider;

import javax.inject.Inject;

import static java.time.Duration.ofSeconds;
import static pl.allegro.tech.hermes.common.config.Configs.ZOOKEEPER_BASE_SLEEP_TIME;
import static pl.allegro.tech.hermes.common.config.Configs.ZOOKEEPER_MAX_RETRIES;
import static pl.allegro.tech.hermes.common.config.Configs.ZOOKEEPER_MAX_SLEEP_TIME_IN_SECONDS;

public class ZookeeperClientManagerFactory implements Factory<ZookeeperClientManager> {

    private final ConfigFactory configFactory;
    private final DcNameProvider dcNameProvider;

    @Inject
    public ZookeeperClientManagerFactory(ConfigFactory configFactory, DcNameProvider dcNameProvider) {
        this.configFactory = configFactory;
        this.dcNameProvider = dcNameProvider;
    }

    @Override
    public ZookeeperClientManager provide() {
        ZookeeperProperties properties = buildZookeeperProperties();
        return new ZookeeperClientManager(properties, dcNameProvider);
    }

    private ZookeeperProperties buildZookeeperProperties() {
        ZookeeperProperties properties = new ZookeeperProperties();
        properties.setPathPrefix(configFactory.getStringProperty(Configs.ZOOKEEPER_ROOT));
        properties.setRetryTimes(configFactory.getIntProperty(ZOOKEEPER_MAX_RETRIES));
        properties.setRetrySleep(configFactory.getIntProperty(ZOOKEEPER_BASE_SLEEP_TIME));
        int maxRetrySleep = Ints.saturatedCast(
                ofSeconds(configFactory.getIntProperty(ZOOKEEPER_MAX_SLEEP_TIME_IN_SECONDS)).toMillis()
        );
        properties.setMaxRetrySleep(maxRetrySleep);
        // TODO finish rest of the mapping
        return properties;
    }

    @Override
    public void dispose(ZookeeperClientManager instance) {

    }
}
