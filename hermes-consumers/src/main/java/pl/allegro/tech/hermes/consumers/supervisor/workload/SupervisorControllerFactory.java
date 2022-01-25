package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.glassfish.hk2.api.Factory;//TODO: remove
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.SelectiveSupervisorController;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_ASSIGNMENT_PROCESSING_THREAD_POOL_SIZE;

public class SupervisorControllerFactory implements Factory<SupervisorController> {//TODO: remove all Factories and instead use java config with factory methods

    private final SupervisorController supervisorController;

    @Inject
    public SupervisorControllerFactory(InternalNotificationsBus notificationsBus,
                                       ConsumerNodesRegistry consumerNodesRegistry,
                                       ConsumerAssignmentRegistry assignmentRegistry,
                                       ConsumerAssignmentCache consumerAssignmentCache,
                                       ClusterAssignmentCache clusterAssignmentCache,
                                       SubscriptionsCache subscriptionsCache,
                                       ConsumersSupervisor supervisor,
                                       ZookeeperAdminCache adminCache,
                                       HermesMetrics metrics,
                                       ConfigFactory configs,
                                       WorkloadConstraintsRepository workloadConstraintsRepository) {
        this.supervisorController = new SelectiveSupervisorController(supervisor, notificationsBus, subscriptionsCache,
                consumerAssignmentCache, assignmentRegistry, clusterAssignmentCache, consumerNodesRegistry, adminCache,
                getAssignmentExecutor(configs), configs, metrics, workloadConstraintsRepository);
    }

    private ExecutorService getAssignmentExecutor(ConfigFactory configs) {
        Logger logger = LoggerFactory.getLogger(SupervisorControllerFactory.class);
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("AssignmentExecutor-%d")
                .setUncaughtExceptionHandler((t, e) -> logger.error("AssignmentExecutor failed {}", t.getName(), e)).build();
        return newFixedThreadPool(configs.getIntProperty(CONSUMER_WORKLOAD_ASSIGNMENT_PROCESSING_THREAD_POOL_SIZE), threadFactory);
    }

    @Override
    public SupervisorController provide() {
        return supervisorController;
    }

    @Override
    public void dispose(SupervisorController instance) {

    }
}
