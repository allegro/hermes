package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.List;

public class NoOpBalancingListener implements BalancingListener {

    @Override
    public void onBeforeBalancing(List<String> activeConsumers, List<SubscriptionName> activeSubscriptions) {

    }

    @Override
    public void onAfterBalancing(WorkDistributionChanges changes) {

    }
}
