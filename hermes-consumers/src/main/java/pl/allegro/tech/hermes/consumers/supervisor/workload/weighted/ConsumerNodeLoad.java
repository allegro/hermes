package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Map;
import java.util.Objects;

class ConsumerNodeLoad {

    static final ConsumerNodeLoad UNDEFINED = new ConsumerNodeLoad(Map.of());

    private final Map<SubscriptionName, SubscriptionLoad> loadPerSubscription;

    ConsumerNodeLoad(Map<SubscriptionName, SubscriptionLoad> loadPerSubscription) {
        this.loadPerSubscription = loadPerSubscription;
    }

    Map<SubscriptionName, SubscriptionLoad> getLoadPerSubscription() {
        return loadPerSubscription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConsumerNodeLoad that = (ConsumerNodeLoad) o;
        return Objects.equals(loadPerSubscription, that.loadPerSubscription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loadPerSubscription);
    }
}
