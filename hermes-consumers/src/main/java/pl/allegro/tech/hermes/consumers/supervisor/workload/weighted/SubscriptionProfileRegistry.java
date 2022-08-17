package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Map;

public interface SubscriptionProfileRegistry {

    Map<SubscriptionName, SubscriptionProfile> getAll();

    void persist(Map<SubscriptionName, SubscriptionProfile> profiles);
}
