package pl.allegro.tech.hermes.consumers.registry;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import java.time.Clock;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.slf4j.LoggerFactory.getLogger;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_DEAD_AFTER_SECONDS;

public class ConsumerNodesRegistryFactory implements Factory<ConsumerNodesRegistry> {

    private static final Logger logger = getLogger(ConsumerNodesRegistryFactory.class);

    private final CuratorFramework curatorFramework;
    private final ConfigFactory configFactory;
    private final ZookeeperPaths zookeeperPaths;
    private final Clock clock;

    @Inject
    public ConsumerNodesRegistryFactory(CuratorFramework curatorFramework, ConfigFactory configFactory, ZookeeperPaths zookeeperPaths, Clock clock) {
        this.curatorFramework = curatorFramework;
        this.configFactory = configFactory;
        this.zookeeperPaths = zookeeperPaths;
        this.clock = clock;
    }

    @Override
    public ConsumerNodesRegistry provide() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("ConsumerRegistryExecutor-%d").build();

        String clusterName = configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME);
        String consumerNodeId = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_NODE_ID);
        int deadAfterSeconds = configFactory.getIntProperty(CONSUMER_WORKLOAD_DEAD_AFTER_SECONDS);
        ConsumerNodesRegistryPaths registryPaths = new ConsumerNodesRegistryPaths(zookeeperPaths, clusterName);

        return new ConsumerNodesRegistry(curatorFramework, newSingleThreadExecutor(threadFactory),
                registryPaths, consumerNodeId, deadAfterSeconds, clock);
    }

    @Override
    public void dispose(ConsumerNodesRegistry instance) {

    }
}
