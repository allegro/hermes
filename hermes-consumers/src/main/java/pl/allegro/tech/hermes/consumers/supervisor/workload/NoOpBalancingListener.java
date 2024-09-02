package pl.allegro.tech.hermes.consumers.supervisor.workload;

import java.util.List;

public class NoOpBalancingListener implements BalancingListener {

  @Override
  public void onBeforeBalancing(List<String> activeConsumers) {}

  @Override
  public void onAfterBalancing(WorkDistributionChanges changes) {}

  @Override
  public void onBalancingSkipped() {}
}
