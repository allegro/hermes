package pl.allegro.tech.hermes.consumers.subscription.id;

import java.util.Objects;

public class SubscriptionId {

    private final long value;

    private SubscriptionId(long value) {
        this.value = value;
    }

    public static SubscriptionId from(long value) {
        return new SubscriptionId(value);
    }

    public long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionId that = (SubscriptionId) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
