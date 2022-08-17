package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.supervisor.workload.BalancingListener;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkDistributionChanges;

import javax.annotation.concurrent.NotThreadSafe;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.SubscriptionProfile.UNDEFINED;

@NotThreadSafe
public class SubscriptionProfilesCalculator implements SubscriptionProfileProvider, BalancingListener {

    private final ConsumerNodeLoadRegistry consumerNodeLoadRegistry;
    private final SubscriptionProfileRegistry subscriptionProfileRegistry;
    private final Clock clock;
    private final Duration weightWindowSize;

    private final Map<SubscriptionName, WeightWindow> currentWindows = new HashMap<>();
    private final Map<SubscriptionName, SubscriptionProfile> profiles = new HashMap<>();

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
    public void onBeforeBalancing(List<String> activeConsumers, List<SubscriptionName> activeSubscriptions) {
        cleanup(new HashSet<>(activeSubscriptions));
        for (String consumerId : activeConsumers) {
            ConsumerNodeLoad consumerNodeLoad = consumerNodeLoadRegistry.get(consumerId);
            for (Map.Entry<SubscriptionName, SubscriptionLoad> entry : consumerNodeLoad.getLoadPerSubscription().entrySet()) {
                SubscriptionName subscriptionName = entry.getKey();
                Weight currentWeight = new Weight(entry.getValue().getOperationsPerSecond());
                SubscriptionProfile currentProfile = profiles.getOrDefault(subscriptionName, UNDEFINED);
                Weight newWeight = calculateNewWeight(subscriptionName, currentWeight, currentProfile.getWeight());
                profiles.put(entry.getKey(), new SubscriptionProfile(currentProfile.getLastRebalanceTimestamp(), newWeight));
            }
        }
    }

    private void cleanup(Set<SubscriptionName> activeSubscriptions) {
        Map<SubscriptionName, SubscriptionProfile> initProfiles = subscriptionProfileRegistry.getAll();
        profiles.putAll(initProfiles);
        profiles.entrySet().removeIf(e -> !activeSubscriptions.contains(e.getKey()));
        currentWindows.entrySet().removeIf(e -> !activeSubscriptions.contains(e.getKey()));
    }

    private Weight calculateNewWeight(SubscriptionName subscriptionName, Weight currentWeight, Weight previousWeight) {
        WeightWindow currentWindow = currentWindows.computeIfAbsent(subscriptionName, ignore -> createWindow());
        Weight newWeight = Weight.max(currentWindow.getWeight(), currentWeight);
        currentWindow.updateWeight(newWeight);
        if (currentWindow.isFinished(clock.instant())) {
            currentWindows.remove(subscriptionName);
            return newWeight;
        } else {
            return Weight.max(previousWeight, newWeight);
        }
    }

    private WeightWindow createWindow() {
        return new WeightWindow(clock.instant().plus(weightWindowSize));
    }

    @Override
    public void onAfterBalancing(WorkDistributionChanges changes) {
        for (SubscriptionName subscriptionName : changes.getRebalancedSubscriptions()) {
            SubscriptionProfile profile = profiles.get(subscriptionName);
            if (profile != null) {
                profiles.put(subscriptionName, new SubscriptionProfile(clock.instant(), profile.getWeight()));
            }
        }
        subscriptionProfileRegistry.persist(profiles);
    }

    @Override
    public SubscriptionProfile get(SubscriptionName subscriptionName) {
        return profiles.getOrDefault(subscriptionName, UNDEFINED);
    }

    private static class WeightWindow {

        private final Instant deadline;
        private Weight weight = Weight.ZERO;

        WeightWindow(Instant deadline) {
            this.deadline = deadline;
        }

        Weight getWeight() {
            return weight;
        }

        void updateWeight(Weight weight) {
            this.weight = weight;
        }

        boolean isFinished(Instant now) {
            return deadline.isBefore(now);
        }
    }
}
