package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints;

import pl.allegro.tech.hermes.common.config.ConfigFactory;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_CONSUMERS_PER_SUBSCRIPTION;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_MAX_SUBSCRIPTIONS_PER_CONSUMER;

public class DefaultWorkloadConstraintsRepository implements WorkloadConstraintsRepository {

    private final ConfigFactory configFactory;

    public DefaultWorkloadConstraintsRepository(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    @Override
    public WorkloadConstraints getWorkloadConstraints() {
        return WorkloadConstraints.defaultConstraints(
                configFactory.getIntProperty(CONSUMER_WORKLOAD_CONSUMERS_PER_SUBSCRIPTION),
                configFactory.getIntProperty(CONSUMER_WORKLOAD_MAX_SUBSCRIPTIONS_PER_CONSUMER)
        );
    }
}
