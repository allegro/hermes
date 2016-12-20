package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import javax.inject.Named;

import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;
import static pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentRegistry.AUTO_ASSIGNED_MARKER;

public class SubscriptionAssignmentRegistryFactory implements Factory<SubscriptionAssignmentRegistry> {

    private final CuratorFramework curatorClient;

    private final ConfigFactory configFactory;

    private final SubscriptionAssignmentCaches subscriptionAssignmentCaches;

    @Inject
    public SubscriptionAssignmentRegistryFactory(@Named(CuratorType.HERMES) CuratorFramework curatorClient,
                                                 ConfigFactory configFactory,
                                                 SubscriptionAssignmentCaches subscriptionAssignmentCaches) {
        this.curatorClient = curatorClient;
        this.configFactory = configFactory;
        this.subscriptionAssignmentCaches = subscriptionAssignmentCaches;
    }

    @Override
    public SubscriptionAssignmentRegistry provide() {
        ZookeeperPaths paths = new ZookeeperPaths(configFactory.getStringProperty(Configs.ZOOKEEPER_ROOT));
        String cluster = configFactory.getStringProperty(KAFKA_CLUSTER_NAME);

        String consumersRuntimePath = paths.consumersRuntimePath(cluster);
        return new SubscriptionAssignmentRegistry(
                curatorClient,
                subscriptionAssignmentCaches.localClusterCache(),
                new SubscriptionAssignmentPathSerializer(consumersRuntimePath, AUTO_ASSIGNED_MARKER)
        );
    }

    @Override
    public void dispose(SubscriptionAssignmentRegistry instance) {

    }
}
