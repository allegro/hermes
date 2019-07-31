package pl.allegro.tech.hermes.consumers.subscription.id;

import pl.allegro.tech.hermes.api.SubscriptionName;

public class SubscriptionId {

    private final int value;

    private SubscriptionId(SubscriptionName subscriptionName) {
        this(subscriptionName.hashCode());
    }

    private SubscriptionId(int value) {
        this.value = value;
    }

    public static SubscriptionId of(SubscriptionName subscriptionName) {
        return new SubscriptionId(subscriptionName);
    }

    public static SubscriptionId from(int value) {
        return new SubscriptionId(value);
    }

    public int getValue() {
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
        return value;
    }
}
