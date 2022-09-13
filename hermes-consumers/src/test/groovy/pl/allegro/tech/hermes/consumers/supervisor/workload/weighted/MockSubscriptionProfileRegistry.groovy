package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted

import pl.allegro.tech.hermes.api.SubscriptionName

import java.time.Instant

class MockSubscriptionProfileRegistry implements SubscriptionProfileRegistry {

    private SubscriptionProfiles subscriptionProfiles = SubscriptionProfiles.EMPTY

    @Override
    SubscriptionProfiles fetch() {
        return subscriptionProfiles
    }

    @Override
    void persist(SubscriptionProfiles profilesToPersist) {
        subscriptionProfiles = profilesToPersist
    }

    MockSubscriptionProfileRegistry updateTimestamp(Instant updateTimestamp) {
        subscriptionProfiles = new SubscriptionProfiles(subscriptionProfiles.getProfiles(), updateTimestamp)
        return this
    }

    MockSubscriptionProfileRegistry profile(SubscriptionName subscriptionName, Instant lastRebalanceTimestamp, Weight weight) {
        def profiles = new HashMap<>(subscriptionProfiles.getProfiles())
        profiles.put(subscriptionName, new SubscriptionProfile(lastRebalanceTimestamp, weight))
        subscriptionProfiles = new SubscriptionProfiles(profiles, subscriptionProfiles.updateTimestamp)
        return this
    }

    void reset() {
        subscriptionProfiles = SubscriptionProfiles.EMPTY
    }
}
