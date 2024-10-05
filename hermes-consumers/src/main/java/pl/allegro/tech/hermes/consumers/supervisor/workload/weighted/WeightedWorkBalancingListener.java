package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static java.util.stream.Collectors.toMap;

import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.supervisor.workload.BalancingListener;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkDistributionChanges;

public class WeightedWorkBalancingListener implements BalancingListener {

  private final ConsumerNodeLoadRegistry consumerNodeLoadRegistry;
  private final SubscriptionProfileRegistry subscriptionProfileRegistry;
  private final CurrentLoadProvider currentLoadProvider;
  private final WeightedWorkloadMetricsReporter weightedWorkloadMetrics;
  private final SubscriptionProfilesCalculator subscriptionProfilesCalculator;
  private final Clock clock;

  public WeightedWorkBalancingListener(
      ConsumerNodeLoadRegistry consumerNodeLoadRegistry,
      SubscriptionProfileRegistry subscriptionProfileRegistry,
      CurrentLoadProvider currentLoadProvider,
      WeightedWorkloadMetricsReporter weightedWorkloadMetrics,
      Clock clock,
      Duration weightWindowSize) {
    this.consumerNodeLoadRegistry = consumerNodeLoadRegistry;
    this.subscriptionProfileRegistry = subscriptionProfileRegistry;
    this.currentLoadProvider = currentLoadProvider;
    this.weightedWorkloadMetrics = weightedWorkloadMetrics;
    this.subscriptionProfilesCalculator =
        new SubscriptionProfilesCalculator(clock, weightWindowSize);
    this.clock = clock;
  }

  @Override
  public void onBeforeBalancing(List<String> activeConsumers) {
    weightedWorkloadMetrics.unregisterMetricsForConsumersOtherThan(new HashSet<>(activeConsumers));
    Map<String, ConsumerNodeLoad> newConsumerLoads = fetchConsumerNodeLoads(activeConsumers);
    currentLoadProvider.updateConsumerNodeLoads(newConsumerLoads);
    SubscriptionProfiles currentProfiles =
        recalculateSubscriptionProfiles(newConsumerLoads.values());
    currentLoadProvider.updateProfiles(currentProfiles);
  }

  private Map<String, ConsumerNodeLoad> fetchConsumerNodeLoads(List<String> activeConsumers) {
    return activeConsumers.stream()
        .collect(toMap(Function.identity(), consumerNodeLoadRegistry::get));
  }

  private SubscriptionProfiles recalculateSubscriptionProfiles(
      Collection<ConsumerNodeLoad> consumerNodeLoads) {
    SubscriptionProfiles previousProfiles = subscriptionProfileRegistry.fetch();
    return subscriptionProfilesCalculator.calculate(consumerNodeLoads, previousProfiles);
  }

  @Override
  public void onAfterBalancing(WorkDistributionChanges changes) {
    applyRebalanceTimestampToSubscriptionProfiles(changes.getRebalancedSubscriptions());
  }

  private void applyRebalanceTimestampToSubscriptionProfiles(
      Set<SubscriptionName> rebalancedSubscriptions) {
    SubscriptionProfiles currentProfiles = currentLoadProvider.getProfiles();
    Map<SubscriptionName, SubscriptionProfile> profilePerSubscription =
        new HashMap<>(currentProfiles.getProfiles());
    for (SubscriptionName subscriptionName : rebalancedSubscriptions) {
      SubscriptionProfile profile = profilePerSubscription.get(subscriptionName);
      if (profile != null) {
        profilePerSubscription.put(
            subscriptionName, new SubscriptionProfile(clock.instant(), profile.getWeight()));
      }
    }
    SubscriptionProfiles finalProfiles =
        new SubscriptionProfiles(profilePerSubscription, currentProfiles.getUpdateTimestamp());
    subscriptionProfileRegistry.persist(finalProfiles);
    currentLoadProvider.updateProfiles(finalProfiles);
  }

  @Override
  public void onBalancingSkipped() {
    weightedWorkloadMetrics.unregisterLeaderMetrics();
    currentLoadProvider.clear();
  }
}
