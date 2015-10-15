package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_NODE_ID;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;

public class WorkTrackerFactory implements Factory<WorkTracker> {
    private final CuratorFramework curatorClient;
    private final ConfigFactory configFactory;
    private final ObjectMapper objectMapper;
    private final SubscriptionRepository subscriptionRepository;

    @Inject
    public WorkTrackerFactory(@Named(CuratorType.HERMES) CuratorFramework curatorClient,
                              ConfigFactory configFactory,
                              ObjectMapper objectMapper,
                              SubscriptionRepository subscriptionRepository) {
        this.curatorClient = curatorClient;
        this.configFactory = configFactory;
        this.objectMapper = objectMapper;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public WorkTracker provide() {
        ZookeeperPaths paths = new ZookeeperPaths(configFactory.getStringProperty(Configs.ZOOKEEPER_ROOT));
        ExecutorService executorService = newFixedThreadPool(configFactory.getIntProperty(Configs.ZOOKEEPER_CACHE_THREAD_POOL_SIZE));
        String consumerNodeId = configFactory.getStringProperty(CONSUMER_WORKLOAD_NODE_ID);
        String cluster = configFactory.getStringProperty(KAFKA_CLUSTER_NAME);
        return new WorkTracker(curatorClient, objectMapper, paths.consumersRuntimePath(cluster), consumerNodeId, executorService, subscriptionRepository);
    }

    @Override
    public void dispose(WorkTracker instance) {
        try {
            instance.stop();
        } catch (IOException e) {
            throw new InternalProcessingException(e);
        }
    }
}
