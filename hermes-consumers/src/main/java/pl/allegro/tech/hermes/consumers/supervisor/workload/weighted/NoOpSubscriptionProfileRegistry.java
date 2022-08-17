package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Map;

public class NoOpSubscriptionProfileRegistry implements SubscriptionProfileRegistry {

    @Override
    public Map<SubscriptionName, SubscriptionProfile> getAll() {
        return Map.of();
    }

    @Override
    public void persist(Map<SubscriptionName, SubscriptionProfile> profiles) {

    }
}
