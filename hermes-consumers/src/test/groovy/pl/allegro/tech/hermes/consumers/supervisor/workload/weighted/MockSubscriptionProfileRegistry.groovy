package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted

import pl.allegro.tech.hermes.api.SubscriptionName

import java.time.Instant

class MockSubscriptionProfileRegistry implements SubscriptionProfileRegistry, SubscriptionProfileProvider {

    private Instant updateTimestamp
    private final Map<SubscriptionName, SubscriptionProfile> profiles = new HashMap<>()

    @Override
    SubscriptionProfile get(SubscriptionName subscriptionName) {
        return profiles.getOrDefault(subscriptionName, SubscriptionProfile.UNDEFINED)
    }

    @Override
    SubscriptionProfiles fetch() {
        return new SubscriptionProfiles(profiles, updateTimestamp)
    }

    @Override
    void persist(SubscriptionProfiles profiles) {

    }

    Set<SubscriptionName> getSubscriptionNames() {
        return profiles.keySet()
    }

    MockSubscriptionProfileRegistry updateTimestamp(Instant updateTimestamp) {
        this.updateTimestamp = updateTimestamp
        return this
    }

    MockSubscriptionProfileRegistry profile(SubscriptionName subscriptionName, Instant lastRebalanceTimestamp, Weight weight) {
        profiles.put(subscriptionName, new SubscriptionProfile(lastRebalanceTimestamp, weight))
        return this
    }

    void reset() {
        profiles.clear()
        updateTimestamp = null
    }
}
