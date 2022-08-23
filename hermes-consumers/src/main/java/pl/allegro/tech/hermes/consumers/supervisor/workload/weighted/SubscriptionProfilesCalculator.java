package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.supervisor.workload.BalancingListener;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkDistributionChanges;

import javax.annotation.concurrent.NotThreadSafe;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NotThreadSafe
public class SubscriptionProfilesCalculator implements SubscriptionProfileProvider, BalancingListener {

    private final ConsumerNodeLoadRegistry consumerNodeLoadRegistry;
    private final SubscriptionProfileRegistry subscriptionProfileRegistry;
    private final Clock clock;
    private final Duration weightWindowSize;

    private final SubscriptionProfiles profiles = new SubscriptionProfiles();

    public SubscriptionProfilesCalculator(ConsumerNodeLoadRegistry consumerNodeLoadRegistry,
                                          SubscriptionProfileRegistry subscriptionProfileRegistry,
                                          Clock clock,
                                          Duration weightWindowSize) {
        this.consumerNodeLoadRegistry = consumerNodeLoadRegistry;
        this.subscriptionProfileRegistry = subscriptionProfileRegistry;
        this.clock = clock;
        this.weightWindowSize = weightWindowSize;
    }

    @Override
    public void onBeforeBalancing(List<String> activeConsumers) {
        SubscriptionProfiles subscriptionProfiles = subscriptionProfileRegistry.fetch();
        Map<SubscriptionName, WeightCalculator> currentWeights = createWeightCalculators(activeConsumers, subscriptionProfiles);
        profiles.reset(clock.instant());
        for (Map.Entry<SubscriptionName, WeightCalculator> entry : currentWeights.entrySet()) {
            SubscriptionName subscriptionName = entry.getKey();
            WeightCalculator weightCalculator = entry.getValue();
            SubscriptionProfile previousProfile = subscriptionProfiles.getProfile(subscriptionName);
            SubscriptionProfile newProfile = new SubscriptionProfile(
                    previousProfile != null ? previousProfile.getLastRebalanceTimestamp() : null,
                    weightCalculator.calculateExponentiallyWeightedMovingAverage(profiles.getUpdateTimestamp())
            );
            profiles.updateProfile(subscriptionName, newProfile);
        }
    }

    private Map<SubscriptionName, WeightCalculator> createWeightCalculators(List<String> activeConsumers,
                                                                            SubscriptionProfiles subscriptionProfiles) {
        Map<SubscriptionName, WeightCalculator> weightCalculators = new HashMap<>();
        for (String consumerId : activeConsumers) {
            ConsumerNodeLoad consumerNodeLoad = consumerNodeLoadRegistry.get(consumerId);
            for (Map.Entry<SubscriptionName, SubscriptionLoad> entry : consumerNodeLoad.getLoadPerSubscription().entrySet()) {
                SubscriptionName subscriptionName = entry.getKey();
                Weight currentWeight = new Weight(entry.getValue().getOperationsPerSecond());
                WeightCalculator weightCalculator = weightCalculators.computeIfAbsent(
                        subscriptionName,
                        subscription -> createWeightCalculator(subscriptionProfiles, subscriptionName)
                );
                weightCalculator.update(currentWeight);
            }
        }
        return weightCalculators;
    }

    private WeightCalculator createWeightCalculator(SubscriptionProfiles subscriptionProfiles, SubscriptionName subscriptionName) {
        SubscriptionProfile subscriptionProfile = subscriptionProfiles.getProfile(subscriptionName);
        return new WeightCalculator(
                weightWindowSize,
                subscriptionProfile != null ? subscriptionProfile.getWeight() : null,
                subscriptionProfiles.getUpdateTimestamp()
        );
    }

    @Override
    public void onAfterBalancing(WorkDistributionChanges changes) {
        for (SubscriptionName subscriptionName : changes.getRebalancedSubscriptions()) {
            SubscriptionProfile profile = profiles.getProfile(subscriptionName);
            if (profile != null) {
                profiles.updateProfile(subscriptionName, new SubscriptionProfile(clock.instant(), profile.getWeight()));
            }
        }
        subscriptionProfileRegistry.persist(profiles);
    }

    @Override
    public SubscriptionProfile get(SubscriptionName subscriptionName) {
        return profiles.getProfileOrUndefined(subscriptionName);
    }

    private static class WeightCalculator {

        private final Duration weightWindowSize;
        private final Weight previousWeight;
        private final Instant previousUpdateTimestamp;
        private Weight currentWeight = Weight.ZERO;

        WeightCalculator(Duration weightWindowSize, Weight previousWeight, Instant previousUpdateTimestamp) {
            this.previousWeight = previousWeight;
            this.weightWindowSize = weightWindowSize;
            this.previousUpdateTimestamp = previousUpdateTimestamp;
        }

        void update(Weight weight) {
            currentWeight = Weight.max(weight, currentWeight);
        }

        Weight calculateExponentiallyWeightedMovingAverage(Instant now) {
            if (previousWeight == null || previousUpdateTimestamp == null) {
                return currentWeight;
            }
            // This calculation is done in the same way as the Linux load average is calculated.
            // See: https://www.helpsystems.com/resources/guides/unix-load-average-part-1-how-it-works
            Duration elapsed = Duration.between(previousUpdateTimestamp, now);
            double alpha = 1.0 - Math.exp(-1.0 * ((double) elapsed.toMillis() / weightWindowSize.toMillis()));
            return currentWeight.multiply(alpha)
                    .add(previousWeight.multiply(1.0 - alpha));
        }
    }
}
