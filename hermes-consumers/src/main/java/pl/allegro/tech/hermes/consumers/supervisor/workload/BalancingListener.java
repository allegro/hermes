package pl.allegro.tech.hermes.consumers.supervisor.workload;

import java.util.List;

public interface BalancingListener {

  void onBeforeBalancing(List<String> activeConsumers);

  void onAfterBalancing(WorkDistributionChanges changes);

  void onBalancingSkipped();
}
