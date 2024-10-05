package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import pl.allegro.tech.hermes.api.SubscriptionName;

class SubscriptionProfilesCalculator {

  private final Clock clock;
  private final Duration weightWindowSize;

  SubscriptionProfilesCalculator(Clock clock, Duration weightWindowSize) {
    this.clock = clock;
    this.weightWindowSize = weightWindowSize;
  }

  SubscriptionProfiles calculate(
      Collection<ConsumerNodeLoad> consumerLoads, SubscriptionProfiles previousProfiles) {
    Map<SubscriptionName, Weight> currentWeights = calculateCurrentWeights(consumerLoads);
    Map<SubscriptionName, SubscriptionProfile> newProfiles = new HashMap<>();
    Instant now = clock.instant();
    for (Map.Entry<SubscriptionName, Weight> entry : currentWeights.entrySet()) {
      SubscriptionName subscriptionName = entry.getKey();
      Weight currentWeight = entry.getValue();
      SubscriptionProfile newProfile =
          applyCurrentWeight(previousProfiles, subscriptionName, currentWeight, now);
      newProfiles.put(subscriptionName, newProfile);
    }
    return new SubscriptionProfiles(newProfiles, now);
  }

  private Map<SubscriptionName, Weight> calculateCurrentWeights(
      Collection<ConsumerNodeLoad> consumerLoads) {
    Map<SubscriptionName, Weight> currentWeights = new HashMap<>();
    for (ConsumerNodeLoad consumerLoad : consumerLoads) {
      for (Map.Entry<SubscriptionName, SubscriptionLoad> entry :
          consumerLoad.getLoadPerSubscription().entrySet()) {
        SubscriptionName subscriptionName = entry.getKey();
        Weight currentConsumerWeight = new Weight(entry.getValue().getOperationsPerSecond());
        Weight currentMaxWeight =
            currentWeights.computeIfAbsent(subscriptionName, subscription -> Weight.ZERO);
        Weight newMaxWeight = Weight.max(currentMaxWeight, currentConsumerWeight);
        currentWeights.put(subscriptionName, newMaxWeight);
      }
    }
    return currentWeights;
  }

  private SubscriptionProfile applyCurrentWeight(
      SubscriptionProfiles previousProfiles,
      SubscriptionName subscriptionName,
      Weight currentWeight,
      Instant now) {
    SubscriptionProfile previousProfile = previousProfiles.getProfile(subscriptionName);
    Weight previousWeight = previousProfile.getWeight();
    ExponentiallyWeightedMovingAverage average =
        new ExponentiallyWeightedMovingAverage(weightWindowSize);
    average.update(previousWeight.getOperationsPerSecond(), previousProfiles.getUpdateTimestamp());
    double opsAvg = average.update(currentWeight.getOperationsPerSecond(), now);
    return new SubscriptionProfile(previousProfile.getLastRebalanceTimestamp(), new Weight(opsAvg));
  }
}
