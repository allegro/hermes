package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.List;

public interface BalancingListener {

    void onBeforeBalancing(List<String> activeConsumers, List<SubscriptionName> activeSubscriptions);

    void onAfterBalancing(WorkDistributionChanges changes);
}
