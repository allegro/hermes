package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.SubscriptionProfile.UNDEFINED;

class SubscriptionProfiles {

    static final SubscriptionProfiles EMPTY = new SubscriptionProfiles(Map.of(), null);

    private final Map<SubscriptionName, SubscriptionProfile> profiles;
    private Instant updateTimestamp;

    SubscriptionProfiles() {
        this(new HashMap<>(), null);
    }

    SubscriptionProfiles(Map<SubscriptionName, SubscriptionProfile> profiles, Instant updateTimestamp) {
        this.profiles = profiles;
        this.updateTimestamp = updateTimestamp;
    }

    Instant getUpdateTimestamp() {
        return updateTimestamp;
    }

    Set<SubscriptionName> getSubscriptions() {
        return profiles.keySet();
    }

    SubscriptionProfile getProfile(SubscriptionName subscriptionName) {
        return profiles.get(subscriptionName);
    }

    SubscriptionProfile getProfileOrUndefined(SubscriptionName subscriptionName) {
        return profiles.getOrDefault(subscriptionName, UNDEFINED);
    }

    void updateProfile(SubscriptionName subscriptionName, SubscriptionProfile newProfile) {
        profiles.put(subscriptionName, newProfile);
    }

    void reset(Instant newUpdateTimestamp) {
        profiles.clear();
        updateTimestamp = newUpdateTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubscriptionProfiles that = (SubscriptionProfiles) o;
        return Objects.equals(profiles, that.profiles)
                && Objects.equals(toMillis(updateTimestamp), toMillis(that.updateTimestamp));
    }

    @Override
    public int hashCode() {
        return Objects.hash(profiles, toMillis(updateTimestamp));
    }

    private Long toMillis(Instant timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toEpochMilli();
    }
}
