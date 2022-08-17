package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted

import pl.allegro.tech.hermes.api.SubscriptionName

import java.time.Instant

class MockSubscriptionProfileRegistry implements SubscriptionProfileRegistry, SubscriptionProfileProvider {

    private final Map<SubscriptionName, SubscriptionProfile> profiles = new HashMap<>()

    @Override
    SubscriptionProfile get(SubscriptionName subscriptionName) {
        return profiles.getOrDefault(subscriptionName, SubscriptionProfile.UNDEFINED)
    }

    @Override
    Map<SubscriptionName, SubscriptionProfile> getAll() {
        return profiles
    }

    @Override
    void persist(Map<SubscriptionName, SubscriptionProfile> profiles) {
        this.profiles.clear()
        this.profiles.putAll(profiles)
    }

    Set<SubscriptionName> getSubscriptionNames() {
        return profiles.keySet()
    }

    MockSubscriptionProfileRegistry profile(SubscriptionName subscriptionName, Instant lastRebalanceTimestamp, Weight weight) {
        profiles.put(subscriptionName, new SubscriptionProfile(lastRebalanceTimestamp, weight))
        return this
    }

    void reset() {
        profiles.clear()
    }
}
