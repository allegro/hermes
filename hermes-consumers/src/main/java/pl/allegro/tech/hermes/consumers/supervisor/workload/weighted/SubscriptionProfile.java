package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import java.time.Instant;
import java.util.Objects;

class SubscriptionProfile {

    static SubscriptionProfile UNDEFINED = new SubscriptionProfile(null, Weight.ZERO);

    private final Instant lastRebalanceTimestamp;
    private final Weight weight;

    SubscriptionProfile(Instant lastRebalanceTimestamp, Weight weight) {
        this.lastRebalanceTimestamp = lastRebalanceTimestamp;
        this.weight = weight;
    }

    Weight getWeight() {
        return weight;
    }

    Instant getLastRebalanceTimestamp() {
        return lastRebalanceTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubscriptionProfile profile = (SubscriptionProfile) o;
        return Objects.equals(weight, profile.weight)
                && Objects.equals(toMillis(lastRebalanceTimestamp), toMillis(profile.lastRebalanceTimestamp));
    }

    @Override
    public int hashCode() {
        return Objects.hash(toMillis(lastRebalanceTimestamp), weight);
    }

    private Long toMillis(Instant timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toEpochMilli();
    }
}
