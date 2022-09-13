package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted

import pl.allegro.tech.hermes.api.SubscriptionName

import java.time.Instant

class SubscriptionProfilesBuilder {

    private final Map<SubscriptionName, SubscriptionProfile> profiles = new HashMap<>()
    private Instant rebalanceTimestamp

    SubscriptionProfilesBuilder withRebalanceTimestamp(Instant rebalanceTimestamp) {
        this.rebalanceTimestamp = rebalanceTimestamp
        return this
    }

    SubscriptionProfilesBuilder withProfile(SubscriptionName subscriptionName, Weight weight) {
        profiles.put(subscriptionName, new SubscriptionProfile(rebalanceTimestamp, weight))
        return this
    }

    SubscriptionProfiles build() {
        return new SubscriptionProfiles(profiles, Instant.MIN)
    }
}
