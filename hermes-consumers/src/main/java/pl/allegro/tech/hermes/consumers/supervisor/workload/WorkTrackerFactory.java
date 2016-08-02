package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_NODE_ID;

public class WorkTrackerFactory implements Factory<WorkTracker> {

    private final ConfigFactory configFactory;
    private final SubscriptionAssignmentRegistry assignementRegistry;

    @Inject
    public WorkTrackerFactory(ConfigFactory configFactory,
                              SubscriptionAssignmentRegistry assignementRegistry) {
        this.configFactory = configFactory;
        this.assignementRegistry = assignementRegistry;
    }

    @Override
    public WorkTracker provide() {
        String consumerNodeId = configFactory.getStringProperty(CONSUMER_WORKLOAD_NODE_ID);
        return new WorkTracker(consumerNodeId, assignementRegistry);
    }

    @Override
    public void dispose(WorkTracker instance) {
    }
}
